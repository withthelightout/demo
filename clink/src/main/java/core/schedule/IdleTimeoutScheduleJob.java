package core.schedule;

import core.Connector;
import core.SchedulerJob;

import java.util.concurrent.TimeUnit;

/**
 * Created by wangkang on 19/7/29.
 */
public class IdleTimeoutScheduleJob extends SchedulerJob {

    public IdleTimeoutScheduleJob(long idleTimeoutMilliseconds, TimeUnit unit, Connector connector) {
        super(idleTimeoutMilliseconds, unit, connector);
    }

    @Override
    public void run() {
        long lastActiveTime = connector.getLastActiveTime();

        System.out.println(lastActiveTime);
        long idleTimeoutMilliseconds = this.idleTimeoutMilliseconds;
        long nextDelay = idleTimeoutMilliseconds - (System.currentTimeMillis() - lastActiveTime);
        if (nextDelay <= 0) {
            scheduler(idleTimeoutMilliseconds);

            try {
                connector.fireIdleTimeoutEvent();
            } catch (Throwable throwable) {
                connector.fireExceptionCaught(throwable);
            }
        } else {
            scheduler(nextDelay);
        }
    }
}
