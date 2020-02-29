package impl;

import core.Scheduler;

import java.io.IOException;
import java.util.concurrent.*;

/**
 * Created by wangkang on 19/7/29.
 */
public class SchedulerImpl implements Scheduler {

    private final ScheduledExecutorService scheduledExecutorService;
    private final ExecutorService deliveryPool;

    public SchedulerImpl(int poolSize) {
        scheduledExecutorService = Executors.newScheduledThreadPool(poolSize, new NameableThreadFactory("Scheduler-Thread"));
        this.deliveryPool = Executors.newFixedThreadPool(4,new NameableThreadFactory("Delivery-Thread-"));
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable runnable, long delay, TimeUnit unit) {
        return scheduledExecutorService.schedule(runnable, delay, unit);
    }

    //用于接收到的消息接着去发送
    @Override
    public void delivery(Runnable runnable) {
        deliveryPool.execute(runnable);
    }

    @Override
    public void close() throws IOException {
        scheduledExecutorService.shutdownNow();
        deliveryPool.shutdownNow();
    }


}
