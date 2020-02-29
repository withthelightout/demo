package impl.async;

import Utils.CloseUtils;
import core.IoArgs;
import core.SendDispatcher;
import core.SendPacket;
import core.Sender;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by wangkang on 19/7/17.
 */
public class AsyncSendDispatcher implements SendDispatcher, IoArgs.IoArgsEventProcessor, AsyncPacketReader.PacketProvider {
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final Sender sender;
    private final Queue<SendPacket> queue = new ConcurrentLinkedQueue<SendPacket>();
    private final AtomicBoolean isSending = new AtomicBoolean();


    private final AsyncPacketReader reader = new AsyncPacketReader(this);


    public AsyncSendDispatcher(Sender sender) {
        this.sender = sender;
        sender.setSendListener(this);
    }


    @Override
    public void send(SendPacket sendPacket) {

        queue.offer(sendPacket);
        RequestSend();


    }

    private void RequestSend() {
        synchronized (isSending) {
            if (isSending.get() || isClosed.get()) {
                return;
            }
            if (reader.requestTakePacket()) {
                try {
                    isSending.set(true);
                    boolean isSuccess = sender.PostSendAsync();
                    if (!isSuccess) {
                        isSending.set(false);
                    }
                } catch (IOException e) {
                    closeAndNotify();
                }
            }
        }


    }

    private void closeAndNotify() {

    }

    @Override
    public SendPacket takePacket() {
        SendPacket sendPacket = queue.poll();
        if (sendPacket == null) {
            return null;
        }


        if (sendPacket.isCanceled())

        {
            return takePacket();
        }

        return sendPacket;
    }

    @Override
    public void completedPacket(SendPacket packet, boolean isSucceed) {
        CloseUtils.close(packet);
    }


    @Override
    public void cancel(SendPacket sendPacket) {
        boolean ret = queue.remove(sendPacket);

        if (ret) {
            sendPacket.cancel();
            return;
        }
        reader.cannel(sendPacket);
    }

    @Override
    public void sendHeartbeat() {
        if (queue.size() > 0) {
            return;
        }
        if (reader.requestSendHeartbeatFrame()) {
            RequestSend();
        }
    }


    @Override
    public void close() throws IOException {
        if (isClosed.compareAndSet(false, true)) {
            //异常关闭
            reader.close();
            queue.clear();
            synchronized (isSending) {
                isSending.set(false);
            }
        }
    }

    @Override
    public IoArgs providerIoArgs() {
        return isClosed.get() ? null : reader.fillData();


    }


    @Override
    public void onConsumeFailed(IoArgs args, Exception e) {
        e.printStackTrace();
        synchronized (isSending) {
            isSending.set(false);
        }
        RequestSend();
    }

    @Override
    public void onConsumeCompleted(IoArgs args) {

        synchronized (isSending) {
            isSending.set(false);
        }
        RequestSend();
    }
}
