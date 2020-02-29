package core;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by wangkang on 19/7/17.
 */
public abstract class Packet<Stream extends Closeable> implements Closeable {
    private Stream stream;

    public static final byte TYPE_MEMORY_BYTES = 1;

    public static final byte TYPE_MEMORY_STRING = 2;

    public static final byte TYPE_MEMORY_FILE = 3;

    public static final byte TYPE_MEMORY_DIRECT = 4;


    protected long length;

    public long length() {
        return length;
    }


    protected abstract Stream createStream();

    protected void closeStream(Stream stream) throws IOException {
        stream.close();
    }


    public final Stream open() {
        if (stream == null) {
            stream = createStream();
        }
        return stream;
    }

    public abstract byte type();

    @Override
    public void close() throws IOException {
        if (stream != null) {
            closeStream(stream);
            stream = null;
        }
    }

    public byte[] headerInfo() {
        return null;
    }
}
