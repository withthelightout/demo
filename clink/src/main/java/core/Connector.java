package core;

import Utils.CloseUtils;
import box.ByteReceivePacket;
import box.FileReceivePacket;
import box.StringReceivePacket;
import box.StringSendPacket;
import impl.SocketChannelAdapter;
import impl.async.AsyncReceiveDispatcher;
import impl.async.AsyncSendDispatcher;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by wangkang on 19/7/16.
 */
public abstract class Connector implements Closeable, SocketChannelAdapter.OnChannelStatusChangedListener {
    protected UUID key = UUID.randomUUID();
    private SocketChannel channel;
    private Sender sender;
    private Receiver receiver;
    private SendDispatcher sendDispatcher;
    private ReceiveDispatcher receiveDispatcher;
    private final List<SchedulerJob> scheduleJobs = new ArrayList<SchedulerJob>(4);

    public void setup(SocketChannel socketChannel) throws IOException {
        this.channel = socketChannel;
        IoContext context = IoContext.get();
        SocketChannelAdapter adapter = new SocketChannelAdapter(channel, context.getIoProvider(), this);

        this.sender = adapter;
        this.receiver = adapter;

        sendDispatcher = new AsyncSendDispatcher(sender);
        receiveDispatcher = new AsyncReceiveDispatcher(receiver, receivePacketCallback);

        receiveDispatcher.start();

    }

    public void send(String msg) {
        SendPacket sendPacket = new StringSendPacket(msg);
        sendDispatcher.send(sendPacket);
    }

    public void send(SendPacket packet) {
        sendDispatcher.send(packet);
    }

    public void schedule(SchedulerJob job) {
        synchronized (scheduleJobs) {
            if (scheduleJobs.contains(job)) {
                return;
            }
            IoContext context = IoContext.get();
            Scheduler scheduler = context.getScheduler();

            job.scheduler(scheduler);
            scheduleJobs.add(job);
        }
    }


    private ReceiveDispatcher.ReceivePacketCallback receivePacketCallback = new ReceiveDispatcher.ReceivePacketCallback() {
        @Override
        public void onReceivePacketComplated(ReceivePacket packet) {
            onReceivedPacket(packet);
        }

        @Override
        public ReceivePacket<?, ?> onArrivedNewPacket(byte type, long length) {
            switch (type) {
                case Packet.TYPE_MEMORY_BYTES:
                    return new ByteReceivePacket(length);
                case Packet.TYPE_MEMORY_STRING:
                    return new StringReceivePacket(length);
                case Packet.TYPE_MEMORY_FILE:
                    return new FileReceivePacket(length, createNewReceiveFile());
                case Packet.TYPE_MEMORY_DIRECT:
                    return new ByteReceivePacket(length);
                default:
                    throw new UnsupportedOperationException("unsupport type" + type);
            }
        }

        @Override
        public void onReceivedHeartbeat() {
            System.out.println(key.toString() + "[Heartbeat]");
        }
    };

    protected abstract File createNewReceiveFile();


    protected void onReceivedPacket(ReceivePacket packet) {

//        System.out.println(key.toString() + "[NEW packet-type:" + packet.type() + ", Length" + packet.length() + ", " + packet.entity());
    }


    @Override
    public void onChannelClosed(SocketChannel channel) {
        synchronized (scheduleJobs) {
            for (SchedulerJob schedulerjob : scheduleJobs) {
                schedulerjob.unSchedule();
            }
            scheduleJobs.clear();
        }
        CloseUtils.close(this);
    }

    @Override
    public void close() throws IOException {
        receiveDispatcher.close();
        sendDispatcher.close();
        sender.close();
        receiver.close();
        channel.close();
    }

    public long getLastActiveTime() {
        return Math.max(sender.getLastWriteTime(), receiver.getLastReadTime());
    }

    public UUID getKey() {
        return key;
    }

    public void fireIdleTimeoutEvent() {
        sendDispatcher.sendHeartbeat();
    }

    public void fireExceptionCaught(Throwable throwable) {

    }
}
