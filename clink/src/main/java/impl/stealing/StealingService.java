package impl.stealing;

import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.IntFunction;

/**
 * Created by wangkang on 19/7/31.
 */
public class StealingService {
    private final int minSafetyThreshold;

    private final StealingSelectorThread[] threads;

    private final LinkedBlockingQueue<IoTask>[] queues;

    private volatile boolean isTerminated = false;


    public StealingService(StealingSelectorThread[] threads, int minSafetyThreshold) {
        this.minSafetyThreshold = minSafetyThreshold;
        this.threads = threads;
        this.queues = Arrays.stream(threads).map(StealingSelectorThread::getReadyTaskQueue).toArray((IntFunction<LinkedBlockingQueue<IoTask>[]>) LinkedBlockingQueue[]::new);

    }

    public void shudown() {
        if (isTerminated) {
            return;
        }
        isTerminated = true;
        for (StealingSelectorThread thread : threads) {
            thread.exit();
        }
    }

    public boolean isTerminated() {
        return isTerminated;
    }

    public void execute(IoTask ioTask) {

    }

    IoTask steal(final LinkedBlockingQueue<IoTask> excludeQueue) {
        final int minSafeyThreshold = this.minSafetyThreshold;
        final LinkedBlockingQueue<IoTask>[] queues = this.queues;
        for (LinkedBlockingQueue<IoTask> queue : queues) {
            if (queue == excludeQueue) {
                continue;
            }
            int size = queue.size();
            if (size > minSafeyThreshold) {
                IoTask poll = queue.poll();
                if (poll != null) {
                    return poll;
                }
            }
        }
        return null;
    }

    public StealingSelectorThread getNotBusyThread() {
        StealingSelectorThread targetThread = null;
        long targetKeyCount = Long.MAX_VALUE;
        for (StealingSelectorThread thread : threads) {
            long RegisterTaskQueueSize = thread.getReadyTaskQueue().size();
            if (RegisterTaskQueueSize != -1 && RegisterTaskQueueSize < targetKeyCount) {
                targetKeyCount = RegisterTaskQueueSize;
                targetThread = thread;
            }
        }
        return targetThread;
    }
}
