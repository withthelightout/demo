package core;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by wangkang on 19/7/29.
 */
public abstract class SchedulerJob implements Runnable {
    protected final long idleTimeoutMilliseconds;
    protected final Connector connector;

    private volatile Scheduler scheduler;
    private volatile ScheduledFuture scheduledFuture;

    public SchedulerJob(long idleTimeoutMilliseconds, TimeUnit unit, Connector connector) {
        this.idleTimeoutMilliseconds = unit.toMillis(idleTimeoutMilliseconds);
        this.connector = connector;
    }

    synchronized void scheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
        scheduler(idleTimeoutMilliseconds);
    }

    synchronized void unSchedule() {
        if (scheduler != null) {
            scheduler = null;
        }
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
            scheduledFuture = null;
        }
    }

    protected synchronized void scheduler(long timoutMillseconds) {
        if (scheduler != null) {
            scheduler.schedule(this, timoutMillseconds, TimeUnit.MILLISECONDS);
        }
    }
}
