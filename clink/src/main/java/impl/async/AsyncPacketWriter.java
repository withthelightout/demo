package impl.async;

import core.Frame;
import core.IoArgs;
import core.ReceivePacket;
import frames.*;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Collection;
import java.util.HashMap;

/**
 * Created by wangkang on 19/7/27.
 */
public class AsyncPacketWriter implements Closeable {
    private final HashMap<Short, PacketModel> packetMap = new HashMap<Short, PacketModel>();
    private final IoArgs args = new IoArgs();
    private volatile Frame frameTemp;

    private final PacketProvider packetProvider;

    AsyncPacketWriter(PacketProvider packetProvider) {
        this.packetProvider = packetProvider;
    }

    public void consumeIoArgs(IoArgs args) {
        if (frameTemp == null) {
            Frame temp;
            do {
                temp = buildNewFrame(args);
            } while (temp == null && args.remained());

            if (temp == null) return;

            frameTemp = temp;

            if (!args.remained()) return;
        }

        Frame currentFrame = frameTemp;

        do {
            try {
                if (currentFrame.handle(args)) {
                    if (currentFrame instanceof ReceiveHeaderFrame) {
                        ReceiveHeaderFrame headerFrame = (ReceiveHeaderFrame) currentFrame;
                        ReceivePacket packet = packetProvider.takePacket(headerFrame.getPacketType(),
                                headerFrame.getPacketLength(),
                                headerFrame.getPacketHeaderInfo());
                        appendNewPacket(headerFrame.getBodyIdentifier(), packet);
                    } else if (currentFrame instanceof ReceiveEntityFrame) {
                        completeEntityFrame((ReceiveEntityFrame) currentFrame);
                    }

                    frameTemp = null;
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } while (args.remained());

    }

    private void completeEntityFrame(ReceiveEntityFrame currentFrame) {
        synchronized (packetMap) {
            short identifer = currentFrame.getBodyIdentifier();
            int length = currentFrame.getBodyLength();
            PacketModel model = packetMap.get(identifer);
            if (model == null) return;
            model.unreceivedLength -= length;
            if (model.unreceivedLength <= 0) {
                packetProvider.completedPacket(model.packet, true);
                packetMap.remove(identifer);
            }
        }
    }

    private void appendNewPacket(short bodyIdentifier, ReceivePacket packet) {
        synchronized (packetMap) {
            PacketModel model = new PacketModel(packet);
            packetMap.put(bodyIdentifier, model);
        }
    }

    private Frame buildNewFrame(IoArgs args) {
        AbsReceiveFrame frame = ReceiveFrameFactory.createInstance(args);
        if (frame instanceof CancelReceiveFrame) {
            cancelReceivePacket(frame.getBodyIdentifier());
        } else if (frame instanceof HeartbeatReceiveFrame) {
            packetProvider.onReceivedHeartbeat();
        } else if (frame instanceof ReceiveEntityFrame) {
            WritableByteChannel channel = getPacketChannel(frame.getBodyIdentifier());
            ((ReceiveEntityFrame)frame).bindPacketChannel(channel);
        }
        return frame;
    }

    private WritableByteChannel getPacketChannel(short bodyIdentifier) {
        synchronized (packetMap) {
            PacketModel packetModel = packetMap.get(bodyIdentifier);
            return packetModel == null ? null : packetModel.channel;
        }
    }

    private void cancelReceivePacket(short bodyIdentifier) {
        synchronized (packetMap) {
            PacketModel model = packetMap.get(bodyIdentifier);
            if (model != null) {
                ReceivePacket packet = model.packet;
                packetProvider.completedPacket(packet, false);

            }
        }
    }

    synchronized IoArgs takeIoArgs() {
        args.limit(frameTemp == null
                ? Frame.FRAME_HEADER_LENGTH
                : frameTemp.getConsumableLength());
        return args;
    }

    @Override
    public void close() throws IOException {
        synchronized (packetMap) {
            Collection<PacketModel> values = packetMap.values();
            for (PacketModel value : values) {
                packetProvider.completedPacket(value.packet, false);
            }
            packetMap.clear();
        }
    }


    interface PacketProvider {

        ReceivePacket takePacket(byte type, long length, byte[] headerInfo);


        void completedPacket(ReceivePacket packet, boolean isSucceed);

        void onReceivedHeartbeat();
    }

    static class PacketModel {
        final ReceivePacket packet;
        final WritableByteChannel channel;
        volatile long unreceivedLength;

        PacketModel(ReceivePacket<?, ?> packet) {
            this.packet = packet;
            this.channel = Channels.newChannel(packet.open());
            this.unreceivedLength = packet.length();
        }
    }
}
