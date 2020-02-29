package core;

import java.io.Closeable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by wangkang on 19/7/29.
 */
public interface Scheduler extends Closeable {

    ScheduledFuture<?> schedule(Runnable runnable, long delay, TimeUnit unit);

    void delivery(Runnable runnable);
}
