package core;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by wangkang on 19/7/16.
 */
public interface Receiver extends Closeable {

    void setReceiveListener(IoArgs.IoArgsEventProcessor processor)throws IOException;

    boolean PostReceiveAsync() throws IOException;

    long getLastReadTime();
}
