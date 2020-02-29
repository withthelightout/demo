package impl;

import Utils.CloseUtils;
import core.IoArgs;
import core.IoProvider;
import core.Receiver;
import core.Sender;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by wangkang on 19/7/16.
 */
public class SocketChannelAdapter implements Sender, Receiver, Closeable {
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final SocketChannel channel;
    private final IoProvider ioProvider;
    private final OnChannelStatusChangedListener listener;

    private IoArgs.IoArgsEventProcessor receiveIoEventProcesser;
    private IoArgs.IoArgsEventProcessor sendIoEventProcesser;

    private volatile long lastReadTime = System.currentTimeMillis();
    private volatile long lastWriteTime = System.currentTimeMillis();


    public SocketChannelAdapter(SocketChannel channel, IoProvider ioProvider,
                                OnChannelStatusChangedListener listener) throws IOException {
        this.channel = channel;
        this.ioProvider = ioProvider;
        this.listener = listener;

        channel.configureBlocking(false);
    }

    @Override
    public void setReceiveListener(IoArgs.IoArgsEventProcessor processor) throws IOException {
        receiveIoEventProcesser = processor;
    }

    @Override
    public boolean PostReceiveAsync() throws IOException {

        if (isClosed.get()) {
            throw new IOException("Current channel is closed!");
        }

        inputCallback.checkAttachNull();
        return ioProvider.registerInput(channel, inputCallback);
    }

    @Override
    public long getLastReadTime() {
        return lastReadTime;
    }

    @Override
    public void setSendListener(IoArgs.IoArgsEventProcessor processor) {
        sendIoEventProcesser = processor;
    }

    @Override
    public boolean PostSendAsync() throws IOException {
        if (isClosed.get()) {
            throw new IOException("Current channel is closed!");
        }
        inputCallback.checkAttachNull();
        outputCallback.run();
//        return ioProvider.registerOutput(channel, outputCallback);
        return true;
    }

    @Override
    public long getLastWriteTime() {
        return lastWriteTime;
    }


    @Override
    public void close() throws IOException {
        if (isClosed.compareAndSet(false, true)) {
            // 解除注册回调
            ioProvider.unRegisterInput(channel);
            ioProvider.unRegisterOutput(channel);
            // 关闭
            CloseUtils.close(channel);
            // 回调当前Channel已关闭
            listener.onChannelClosed(channel);
        }
    }


    private final IoProvider.HandleProviderCallback inputCallback = new IoProvider.HandleProviderCallback() {
        @Override
        protected void onProviderIo(IoArgs args) {
            if (isClosed.get()) {
                return;
            }
            lastReadTime = System.currentTimeMillis();

            IoArgs.IoArgsEventProcessor processor = receiveIoEventProcesser;
            if (args == null) {
                args = processor.providerIoArgs();
            }

            try {
                // 具体的读取操作
                if (args == null) {
                    processor.onConsumeFailed(null, new IOException("内容为空"));
                } else {
                    int count = args.readFrom(channel);
                    if (count == 0) {
                        System.out.println("current read zero data");
                    }

                    if (args.remained()) {
                        attach = args;
                        ioProvider.registerInput(channel, this);
                    } else {
                        attach = null;
                        // 读取完成回调
                        processor.onConsumeCompleted(args);
                    }

                }
            } catch (IOException ignored) {
                ignored.printStackTrace();
                CloseUtils.close(SocketChannelAdapter.this);
            }
        }


    };


    private final IoProvider.HandleProviderCallback outputCallback = new IoProvider.HandleProviderCallback() {
        @Override
        protected void onProviderIo(IoArgs args) {
            if (isClosed.get()) {
                return;
            }
            lastWriteTime = System.currentTimeMillis();
            IoArgs.IoArgsEventProcessor processor = sendIoEventProcesser;

            if (args == null) {
                args = processor.providerIoArgs();
            }
            try {
                if (args == null) {
                    processor.onConsumeFailed(null, new IOException("内容为空"));
                } else {
                    int count = args.writeTo(channel);

                    if (count == 0) {
//                        System.out.println("current write zero data");
                    }

                    if (args.remained()) {
                        attach = args;
                        ioProvider.registerOutput(channel, this);
                    } else {
                        attach = null;
                        // 读取完成回调
                        processor.onConsumeCompleted(args);
                    }
                }

            } catch (IOException ignored) {
                CloseUtils.close(SocketChannelAdapter.this);
            }
        }


    };


    public interface OnChannelStatusChangedListener {
        void onChannelClosed(SocketChannel channel);
    }

}
