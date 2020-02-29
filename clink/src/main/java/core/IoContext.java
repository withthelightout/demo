package core;



import java.io.IOException;

/**
 * Created by wangkang on 19/7/16.
 */
public class IoContext {
    private static volatile IoContext INSTANCE = null;


    private final IoProvider ioProvider;
    private final Scheduler scheduler;

    public IoContext(IoProvider ioProvider, Scheduler scheduler) {
        this.ioProvider = ioProvider;
        this.scheduler = scheduler;
    }


    public static StartedBoot setup() {
        return new StartedBoot();
    }



    public IoProvider getIoProvider() {
        return ioProvider;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public static IoContext get() {
        return INSTANCE;
    }


    public static void close() throws IOException {
        if (INSTANCE != null) {
            INSTANCE.callClose();
        }
    }

    private void callClose() throws IOException {
        ioProvider.close();
        scheduler.close();
    }

    public static class StartedBoot {
        private IoProvider ioProvider;
        private Scheduler sheduler;

        private StartedBoot() {
        }

        public StartedBoot ioProvider(IoProvider ioProvider) {
            this.ioProvider = ioProvider;
            return this;
        }

        public StartedBoot sheduler(Scheduler sheduler) {
            this.sheduler = sheduler;
            return this;
        }

        public IoContext start() {
            INSTANCE = new IoContext(ioProvider, sheduler);
            return INSTANCE;
        }

    }


}
