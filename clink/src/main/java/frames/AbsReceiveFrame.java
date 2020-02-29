package frames;

import core.Frame;
import core.IoArgs;

import java.io.IOException;

/**
 * Created by wangkang on 19/7/27.
 */
public abstract class AbsReceiveFrame extends Frame {
    volatile int bodyRemaining;

     AbsReceiveFrame(byte[] header) {
        super(header);
        bodyRemaining = getBodyLength();
    }


    @Override
    public synchronized boolean handle(IoArgs ioArgs) throws IOException {
        if (bodyRemaining == 0) {
            return true;
        }

        bodyRemaining-=consumeBody(ioArgs);
        return bodyRemaining==0;
    }

    protected abstract int consumeBody(IoArgs ioArgs) throws IOException;

    @Override
    public final Frame nextFrame() {
        return null;
    }

    @Override
    public int getConsumableLength() {
        return   bodyRemaining;
    }
}
