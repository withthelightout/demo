package core;

import java.io.InputStream;

/**
 * Created by wangkang on 19/7/17.
 */
public abstract class SendPacket<T extends InputStream> extends Packet<T> {


    private boolean isCanceled;


    public boolean isCanceled() {
        return isCanceled;
    }

    public void cancel() {
        isCanceled = true;
    }

}
