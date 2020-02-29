package core;


import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;

/**
 * Created by wangkang on 19/7/16.
 */
public class IoArgs {
    private int limit = 256;
    private static ByteBuffer buffer = ByteBuffer.allocate(256);
    static {
        buffer.clear();
    }

    //写到buffer里面去
    @SuppressWarnings("Duplicates")
    public int readFrom(ReadableByteChannel channel) throws IOException {


        ByteBuffer buffer = this.buffer;
        int bytesProduced = 0;
        int len = 0;
        do {
            len = channel.read(buffer);
            if (len < 0) {
                throw new EOFException("can not write any data with " + channel);
            }
            bytesProduced += len;
        } while (buffer.hasRemaining() && len != 0);
        return bytesProduced;
    }



    public int writeTo(WritableByteChannel channel) throws IOException {
        ByteBuffer buffer = this.buffer;
        int bytesProduced = 0;
        int len = 0;
        do {
            len = channel.write(buffer);
            if (len < 0) {
                throw new EOFException("can not write any data with " + channel);
            }
            bytesProduced += len;
        } while (buffer.hasRemaining() && len != 0);
        return bytesProduced;
    }


    public int read(SocketChannel channel) throws IOException {
        startWriting();
        int byteProduced = 0;
        while (buffer.hasRemaining()) {
            int len = channel.read(buffer);
            if (len < 0) {
                throw new EOFException();
            }
            byteProduced += len;
        }
        finishWriting();
        return byteProduced;
    }


    public int write(SocketChannel channel) throws IOException {

        int byteProduced = 0;
        while (buffer.hasRemaining()) {
            int len = channel.write(buffer);
            if (len < 0) {
                throw new EOFException();
            }
            byteProduced += len;
        }

        return byteProduced;
    }


    public void startWriting() {

        buffer.clear();
        buffer.limit(limit);
    }

    public void finishWriting() {
        buffer.flip();
    }

    public void limit(int limit) {
        this.limit = Math.min(limit, buffer.capacity());
    }


    public int readLength() {
        return buffer.getInt();
    }

    public int capacity() {
        return buffer.capacity();
    }

    public boolean remained() {
        return buffer.remaining() > 0;
    }

    public int readFrom(byte[] bytes, int offset, int count) {
        int size = Math.min(count, buffer.remaining());
        if (size <= 0) {
            return 0;
        }
        buffer.put(bytes, offset, size);
        return size;
    }

    public int writeTo(byte[] bytes, int offset) {
        int size = Math.min(bytes.length - offset, buffer.remaining());
        buffer.get(bytes, offset, size);
        return size;
    }

    public int fillEmpty(int size) {
        int fillsize = Math.min(size, buffer.remaining());
        buffer.position(buffer.position() + fillsize);
        return fillsize;
    }

    public int setEmpty(int size) {
        int emptySize = Math.min(size, buffer.remaining());
        buffer.position(buffer.position() + emptySize);
        return emptySize;
    }

    public interface IoArgsEventProcessor {
        IoArgs providerIoArgs();

        void onConsumeFailed(IoArgs args, Exception e);

        void onConsumeCompleted(IoArgs args);
    }


}
