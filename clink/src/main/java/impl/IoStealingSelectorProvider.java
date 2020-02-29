package impl;


import core.IoProvider;
import impl.stealing.IoTask;
import impl.stealing.StealingSelectorThread;
import impl.stealing.StealingService;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * Created by wangkang on 19/7/31.
 */
public class IoStealingSelectorProvider implements IoProvider {
    private final IoStealingThread[] threads;
    private final StealingService stealingService;


    public IoStealingSelectorProvider(int poolSize) throws IOException {
        IoStealingThread[] threads = new IoStealingThread[poolSize];
        for (int i = 0; i < poolSize; i++) {
            Selector selector = Selector.open();
            threads[i] = new IoStealingThread("IoStealingSelectorProvider-Thread-", selector);
        }
        StealingService stealingService = new StealingService(threads, 10);
        for (IoStealingThread thread : threads) {
            thread.setStealingService(stealingService);
            thread.start();
        }
        this.stealingService = stealingService;
        this.threads = threads;

    }

    @Override
    public boolean registerInput(SocketChannel channel, HandleProviderCallback callback) {
        StealingSelectorThread thread = stealingService.getNotBusyThread();
        if (thread != null) {
            return thread.register(channel, SelectionKey.OP_READ, callback);
        }
        return false;
    }

    @Override
    public boolean registerOutput(SocketChannel channel, HandleProviderCallback callback) {
        StealingSelectorThread thread = stealingService.getNotBusyThread();
        if (thread != null) {
            return thread.register(channel, SelectionKey.OP_WRITE, callback);
        }
        return false;
    }

    @Override
    public void unRegisterInput(SocketChannel channel) {
        for (IoStealingThread thread : threads) {
            thread.unregister(channel);
        }
    }

    @Override
    public void unRegisterOutput(SocketChannel channel) {

    }

    @Override
    public void close() throws IOException {

    }

    static class IoStealingThread extends StealingSelectorThread {

        public IoStealingThread(String name, Selector selector) {
            super(selector);
            setName(name);
        }

        @Override
        protected boolean processTask(IoTask ioTask) {
            ioTask.providerCallback.run();
            return false;
        }
    }
}
