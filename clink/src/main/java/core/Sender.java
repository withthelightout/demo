package core;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by wangkang on 19/7/16.
 */
public interface Sender extends Closeable {
    void setSendListener(IoArgs.IoArgsEventProcessor processor);

    boolean PostSendAsync() throws IOException;

    long getLastWriteTime();
}
