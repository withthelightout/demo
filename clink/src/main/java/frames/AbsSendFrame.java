package frames;

import core.Frame;
import core.IoArgs;

import java.io.IOException;

/**
 * Created by wangkang on 19/7/27.
 */
public abstract class AbsSendFrame extends Frame {

    volatile byte headerRemaining = Frame.FRAME_HEADER_LENGTH;
    volatile int bodyRemaining;

    public AbsSendFrame(int length, byte type, byte flag, short identifer) {
        super(length, type, flag, identifer);
        bodyRemaining = length;
    }
    public AbsSendFrame(byte[] header) {
        super(header);
    }

    @Override
    public synchronized boolean handle(IoArgs ioArgs) throws IOException {
        try {


            ioArgs.limit(headerRemaining + bodyRemaining);
            ioArgs.startWriting();
            if (headerRemaining > 0 && ioArgs.remained()) {
                headerRemaining -= consumeHeader(ioArgs);
            }
            if (headerRemaining == 0 && ioArgs.remained() && bodyRemaining > 0) {
                bodyRemaining -= consumeBody(ioArgs);
            }

            return headerRemaining == 0 && bodyRemaining == 0;
        } finally {
            ioArgs.finishWriting();
        }
    }

    protected abstract int consumeBody(IoArgs ioArgs) throws IOException;


    private byte consumeHeader(IoArgs ioArgs) {
        int count = headerRemaining;
        int offset = header.length - count;
        return (byte) ioArgs.readFrom(header, offset, count);
    }

    @Override
    public int getConsumableLength() {
        return headerRemaining + bodyRemaining;
    }

    @Override
    public Frame nextFrame() {
        return null;
    }

    protected synchronized boolean isSending() {
        return headerRemaining < Frame.FRAME_HEADER_LENGTH;

    }

}
