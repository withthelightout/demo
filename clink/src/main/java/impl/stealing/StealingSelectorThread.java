package impl.stealing;


import Utils.CloseUtils;
import core.IoProvider;

import java.io.IOException;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by wangkang on 19/7/31.
 */
public abstract class StealingSelectorThread extends Thread {

    private static final int VALID_OPS = SelectionKey.OP_READ | SelectionKey.OP_WRITE;
    private final Selector selector;
    private volatile boolean isRunning = true;
    private final LinkedBlockingQueue<IoTask> readTaskQueue = new LinkedBlockingQueue<IoTask>();


    private final LinkedBlockingQueue<IoTask> registerTaskQueue = new LinkedBlockingQueue<IoTask>();
    private final List<IoTask> onceReadyTaskCache = new ArrayList <IoTask>(200);
    private final AtomicLong saturatingCapacity = new AtomicLong();

    public void setStealingService(StealingService stealingService) {
        this.stealingService = stealingService;
    }

    private volatile StealingService stealingService;

    public StealingSelectorThread(Selector selector) {
        this.selector = selector;
    }

    public boolean register(SocketChannel channel, int ops, IoProvider.HandleProviderCallback callback) {
        if (channel.isOpen()) {
            IoTask ioTask = new IoTask(channel, ops, callback);
            registerTaskQueue.offer(ioTask);
            return true;
        } else {
            return false;
        }
    }

    public void unregister(SocketChannel channel) {
        SelectionKey selectionKey = channel.keyFor(selector);
        if (selectionKey != null && selectionKey.attachment() != null) {
            selectionKey.attach(null);
            IoTask ioTask = new IoTask(channel, 0, null);
            registerTaskQueue.offer(ioTask);
        }
    }



    LinkedBlockingQueue<IoTask> getReadyTaskQueue() {
        return readTaskQueue;
    }

    long getSaturatingCapacity() {
        if (selector.isOpen()) {
            return saturatingCapacity.get();
        } else {
            return -1;
        }
    }



    public void consumeRegisterTodoTasks(final LinkedBlockingQueue<IoTask> registerTaskQueue) {
        final Selector select = this.selector;
        IoTask registerTask = registerTaskQueue.poll();
        while (registerTask != null) {
            try {
                final SocketChannel channel = registerTask.channel;
                int ops = registerTask.ops;
                if (ops == 0) {
                    SelectionKey key = channel.keyFor(selector);
                    if (key != null) {
                        key.cancel();
                    }
                } else if ((ops & ~VALID_OPS) == 0) {
                    SelectionKey key = channel.keyFor(selector);
                    if (key == null) {
                        key = channel.register(selector, ops, new KeyAttachment());
                    } else {
                        key.interestOps(key.interestOps() | ops);
                    }
                    Object attachment = key.attachment();
                    if (attachment instanceof KeyAttachment) {
                        ((KeyAttachment) attachment).attach(ops, registerTask);
                    } else {
                        key.cancel();
                    }
                }
            } catch (ClosedChannelException e) {
                e.printStackTrace();
            } catch (CancelledKeyException e) {
                e.printStackTrace();
            } catch (ClosedSelectorException e) {
                e.printStackTrace();
            } finally {
                registerTask = registerTaskQueue.poll();
            }
        }
    }

    private void joinTaskQueue(final ConcurrentLinkedQueue<IoTask> registerTaskQueue, final List<IoTask> onceReadyTaskCache) {
        readTaskQueue.addAll(onceReadyTaskCache);
    }

    private void consumeTodoTasks(final LinkedBlockingQueue<IoTask> readTaskQueue, LinkedBlockingQueue registerTaskQueue) {
        final AtomicLong saturatingCapacity = this.saturatingCapacity;

        IoTask doTask = readTaskQueue.poll();
        while (doTask != null) {
            saturatingCapacity.incrementAndGet();
            if (processTask(doTask)) {
                registerTaskQueue.offer(doTask);
            }
            doTask = readTaskQueue.poll();
        }

        final StealingService stealingService = this.stealingService;
        if (stealingService != null) {
            doTask = stealingService.steal(readTaskQueue);
            while (doTask != null) {
                saturatingCapacity.incrementAndGet();
                if (processTask(doTask)) {
                    registerTaskQueue.offer(doTask);
                }
                doTask = stealingService.steal(readTaskQueue);

            }
        }
    }

    public void exit() {
        isRunning = false;
        CloseUtils.close(selector);
        interrupt();
    }

    protected abstract boolean processTask(IoTask ioTask);

    @Override
    public final void run() {
        super.run();

        final Selector selector = this.selector;
        final LinkedBlockingQueue<IoTask> readyTaskQueue = this.readTaskQueue;
        final LinkedBlockingQueue<IoTask> registerTaskQueue = this.registerTaskQueue;
        final List<IoTask> onceReadyTaskCache = this.onceReadyTaskCache;

        try {
            while (isRunning) {
                consumeRegisterTodoTasks(registerTaskQueue);
                if ((selector.selectNow()) == 0) {
                    Thread.yield();
                    continue;
                }

                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                while (iterator.hasNext()) {
                    SelectionKey selectionKey = iterator.next();
                    Object attachmentObj = selectionKey.attachment();
                    if (selectionKey.isValid() && attachmentObj instanceof KeyAttachment) {
                        final KeyAttachment attachment = (KeyAttachment) attachmentObj;
                        try {
                            final int readyOps = selectionKey.readyOps();
                            int interestOps = selectionKey.interestOps();

                            if ((readyOps & SelectionKey.OP_READ) != 0) {
                                readyTaskQueue.add(attachment.taskForReadable);
                                interestOps = interestOps & ~SelectionKey.OP_READ;
                            }

                            if ((readyOps & SelectionKey.OP_WRITE) != 0) {
                                readyTaskQueue.add(attachment.taskForWritable);
                                interestOps = interestOps & ~SelectionKey.OP_WRITE;
                            }

                            selectionKey.interestOps(interestOps);

                        } catch (CancelledKeyException e) {
                            readyTaskQueue.remove(attachment.taskForReadable);
                            readyTaskQueue.remove(attachment.taskForWritable);
                        }
                    }
                    iterator.remove();
                }
//                if (!onceReadyTaskCache.isEmpty()) {
//                    joinTaskQueue(readyTaskQueue, onceReadyTaskCache);
//                    onceReadyTaskCache.clear();
//                }

                consumeTodoTasks(readyTaskQueue, registerTaskQueue);

            }
        } catch (ClosedSelectorException e) {
            e.printStackTrace();
        } catch (IOException e) {
            CloseUtils.close(selector);
        } finally {
            readyTaskQueue.clear();
            registerTaskQueue.clear();
            onceReadyTaskCache.clear();
        }

    }

    static class KeyAttachment {
        IoTask taskForReadable;
        IoTask taskForWritable;

        void attach(int ops, IoTask task) {
            if (ops == SelectionKey.OP_READ) {
                taskForReadable = task;
            } else {
                taskForWritable = task;
            }
        }
    }
}
