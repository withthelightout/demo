package impl;

import Utils.CloseUtils;
import core.IoProvider;

import java.io.IOException;
import java.nio.channels.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by wangkang on 19/7/16.
 */
public class IoSelectorProvider implements IoProvider {
    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    private final AtomicBoolean inRegInput = new AtomicBoolean(false);
    private final AtomicBoolean inRegOutput = new AtomicBoolean(false);

    private final Selector readSelector;
    private final Selector writeSelector;

    private final ExecutorService inputHandlePool;
    private final ExecutorService outputHandlePool;

    private final HashMap<SelectionKey, Runnable> inputCallbackMap = new HashMap<SelectionKey, Runnable>();
    private final HashMap<SelectionKey, Runnable> outputCallbackMap = new HashMap<SelectionKey, Runnable>();

    public IoSelectorProvider() throws IOException {
        readSelector = Selector.open();
        writeSelector = Selector.open();

        inputHandlePool = Executors.newFixedThreadPool(4,
                new NameableThreadFactory("IoProvider-Input-Thread-"));
        outputHandlePool = Executors.newFixedThreadPool(4,
                new NameableThreadFactory("IoProvider-Output-Thread-"));
        startRead();
        startWrite();

    }

    private void startWrite() {
        Thread thread = new SelectThread("Clink IoSelectorProvider WriteSelector Thread", isClosed, inRegOutput, writeSelector, outputCallbackMap, outputHandlePool, SelectionKey.OP_WRITE);
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }


    private void startRead() {
        Thread thread = new SelectThread("Clink IoSelectorProvider ReadSelector Thread", isClosed, inRegInput, readSelector, inputCallbackMap, inputHandlePool, SelectionKey.OP_READ);
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    class SelectThread extends Thread {
        private final AtomicBoolean isClosed;
        private final AtomicBoolean locker;
        private final Selector selector;
        private final HashMap<SelectionKey, Runnable> callMap;
        private final ExecutorService pool;
        private final int keyOps;

        public SelectThread(String name, AtomicBoolean isClosed, AtomicBoolean locker, Selector selector, HashMap<SelectionKey, Runnable> callMap, ExecutorService pool, int keyOps) {
            super(name);
            this.isClosed = isClosed;
            this.locker = locker;
            this.selector = selector;
            this.callMap = callMap;
            this.pool = pool;
            this.keyOps = keyOps;
            this.setPriority(Thread.MAX_PRIORITY);
        }

        @Override
        public void run() {
            super.run();
            AtomicBoolean locker = this.locker;
            AtomicBoolean isClosed = this.isClosed;
            Selector selector = this.selector;
            HashMap<SelectionKey, Runnable> callMap = this.callMap;
            ExecutorService pool = this.pool;
            int keyOps = this.keyOps;
            while (!isClosed.get()) {
                try {
                    if (selector.select() == 0) {
                        waitSelection(locker);
                        continue;
                    } else if (locker.get()) {
                        waitSelection(locker);
                    }

                    Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = selectionKeys.iterator();
                    while (iterator.hasNext()) {
                        SelectionKey selectionKey = iterator.next();
                        if (selectionKey.isValid()) {
                            handle(selectionKey, keyOps, callMap, pool, locker);
                        }
                    }
                    selectionKeys.clear();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClosedSelectorException e) {
                    break;
                }
            }
        }
    }

    private void handle(SelectionKey selectKey, int opRead, HashMap<SelectionKey, Runnable> inputCallbackMap, ExecutorService inputHandlePool, AtomicBoolean locker) {
        synchronized (locker) {
            try {
                selectKey.interestOps(selectKey.readyOps() & ~opRead);
            } catch (CancelledKeyException e) {
                return;
            }
        }
        Runnable runnable = null;
        runnable = inputCallbackMap.get(selectKey);
        if (runnable != null && !inputHandlePool.isShutdown()) {
            inputHandlePool.execute(runnable);
        }
    }

    @Override
    public boolean registerInput(SocketChannel channel, HandleProviderCallback callback) {
        return registerSelection(channel, readSelector, SelectionKey.OP_READ, inRegInput,
                inputCallbackMap, callback) != null;
    }

    @Override
    public boolean registerOutput(SocketChannel channel, HandleProviderCallback callback) {
        return registerSelection(channel, writeSelector, SelectionKey.OP_WRITE, inRegOutput,
                outputCallbackMap, callback) != null;
    }

    @Override
    public void unRegisterInput(SocketChannel channel) {
        unRegisterSelection(channel, readSelector, inputCallbackMap, inRegInput);
    }

    private static void unRegisterSelection(SocketChannel channel, Selector selector, HashMap<SelectionKey, Runnable> map, AtomicBoolean locker) {
        synchronized (locker) {
            locker.set(true);
            selector.wakeup();
            try {
                if (channel.isRegistered()) {
                    SelectionKey key = channel.keyFor(selector);
                    if (key != null) {
                        key.cancel();
                        map.remove(key);
                    }
                }
            } finally {
                locker.set(false);
                try {
                    locker.notifyAll();
                } catch (Exception e) {
                }
            }

        }
    }

    @Override
    public void unRegisterOutput(SocketChannel channel) {
        unRegisterSelection(channel, writeSelector, outputCallbackMap, inRegOutput);
    }

    private static void waitSelection(final AtomicBoolean locker) {
        synchronized (locker) {
            if (locker.get()) {
                try {
                    locker.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static SelectionKey registerSelection(SocketChannel channel, Selector selector,
                                                  int registerOps, AtomicBoolean locker,
                                                  HashMap<SelectionKey, Runnable> map,
                                                  Runnable runnable) {
        synchronized (locker) {
            locker.set(true);
            try {
                //TODO 采坑试一下
                selector.wakeup();

                SelectionKey key = null;

                if (channel.isRegistered()) {
                    key = channel.keyFor(selector);
                    if (key != null) {
                        key.interestOps(key.readyOps() | registerOps);
                    }
                }

                if (key == null) {

                    key = channel.register(selector, registerOps);
                    map.put(key, runnable);
                }
                return key;
            } catch (ClosedChannelException e) {
                e.printStackTrace();
                return null;
            } catch (CancelledKeyException e) {
                e.printStackTrace();
                return null;
            } catch (ClosedSelectorException e) {
                e.printStackTrace();
                return null;
            } finally {
                locker.set(false);
                locker.notify();
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (isClosed.compareAndSet(false, true)) {
            inputHandlePool.shutdownNow();
            outputHandlePool.shutdownNow();

            inputCallbackMap.clear();
            outputCallbackMap.clear();


            CloseUtils.close(readSelector, writeSelector);
        }
    }


}
