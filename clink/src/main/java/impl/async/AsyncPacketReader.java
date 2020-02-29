package impl.async;

import core.Frame;
import core.IoArgs;
import core.SendPacket;
import core.ds.BytePriorityNode;
import frames.*;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by wangkang on 19/7/27.
 */
public class AsyncPacketReader implements Closeable {
    private final PacketProvider provider;
    private IoArgs ioargs = new IoArgs();

    // Frame队列
    private volatile BytePriorityNode<Frame> node;

    private volatile int nodeSize = 0;

    // 1,2,3.....255
    private short lastIdentifier = 0;


    AsyncPacketReader(PacketProvider provider) {
        this.provider = provider;
    }

    synchronized void cannel(SendPacket<?> packet) {
        if (nodeSize == 0) {
            return;
        }
        for (BytePriorityNode<Frame> x = node, before = null; x != null; before = x, x = x.next) {
            Frame frame = x.item;
            if (frame instanceof AbsSendPacketFrame) {
                AbsSendPacketFrame packetFrame = (AbsSendPacketFrame) frame;
                if (packetFrame.getPacket() == packet) {
                    boolean removable = packetFrame.abort();
                    if (removable) {
                        // A B C
                        removeFrame(x, before);
                        if (packetFrame instanceof SendHeaderFrame) {
                            // 头帧，并且未被发送任何数据，直接取消后不需要添加取消发送帧
                            break;
                        }
                    }

                    CancelSendFrame cancelSendFrame = new CancelSendFrame(packetFrame.getBodyIdentifier());
                    appendNewFrame(cancelSendFrame);

                    provider.completedPacket(packet, false);
                    break;

                }

            }
        }
    }


    @Override
    public synchronized void close() {
        while (node != null) {
            Frame frame = node.item;
            if (frame instanceof AbsSendPacketFrame) {
                SendPacket packet = ((AbsSendPacketFrame) frame).getPacket();
                provider.completedPacket(packet, false);
            }
            node = node.next;
        }
        nodeSize = 0;
        node = null;
    }

    public IoArgs fillData() {
        Frame currentFrame = getCurrentFrame();
        if (currentFrame == null) return null;

        try {
            if (currentFrame.handle(ioargs)) {
                Frame nextFrame = currentFrame.nextFrame();
                if (nextFrame != null) {
                    appendNewFrame(nextFrame);
                } else if (currentFrame instanceof SendEntityFrame) {
                    provider.completedPacket(((SendEntityFrame) currentFrame).getPacket(), true);
                }
                popCurrentFrame();
            }
            return ioargs;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }

    private synchronized void popCurrentFrame() {
        node = node.next;
        nodeSize--;
        if (node == null) {
            requestTakePacket();
        }
    }

    private synchronized Frame getCurrentFrame() {
        if (node == null) {
            return null;
        }
        return node.item;
    }

        private short generateIdentifier() {
        short identifier = ++lastIdentifier;
        if (identifier == 255) {
            lastIdentifier = 0;
        }
        return identifier;
    }

    boolean requestTakePacket() {
        synchronized (this) {
            if (nodeSize >= 1) {
                return true;
            }
        }

        SendPacket packet = provider.takePacket();
        if (packet != null) {
            short identifier = generateIdentifier();
            SendHeaderFrame frame = new SendHeaderFrame(identifier, packet);
            appendNewFrame(frame);
        }

        synchronized (this) {
            return nodeSize != 0;
        }
    }

    boolean requestSendHeartbeatFrame() {
        synchronized (this) {
            for (BytePriorityNode<Frame> x = node; x != null; x = x.next) {
                Frame frame = x.item;
                if (frame.getBodyType() == Frame.TYPE_COMMAND_HEARTBEAT) {
                    return false;
                }
            }

            appendNewFrame(new HeartbeatSendFrame());
            return true;
        }
    }


    private synchronized void removeFrame(BytePriorityNode<Frame> removeNode, BytePriorityNode<Frame> before) {
        if (before == null) {
            // A B C
            // B C
            node = removeNode.next;
        } else {
            // A B C
            // A C
            before.next = removeNode.next;
        }
        nodeSize--;
        if (node == null) {
            requestTakePacket();
        }
    }

    private void appendNewFrame(Frame frame) {
        BytePriorityNode<Frame> newNode = new BytePriorityNode<Frame>(frame);
        if (node != null) {
            // 使用优先级别添加到链表
            node.appendWithPriority(newNode);
        } else {
            node = newNode;
        }
        nodeSize++;
    }


    interface PacketProvider {

        SendPacket takePacket();


        void completedPacket(SendPacket packet, boolean isSucceed);
    }
}
