package frames;

import core.IoArgs;

import java.io.IOException;

/**
 * Created by wangkang on 19/7/28.
 */
public class CancelReceiveFrame extends AbsReceiveFrame {
    CancelReceiveFrame(byte[] header) {
        super(header);
    }

    @Override
    protected int consumeBody(IoArgs args) throws IOException {
        return 0;
    }
}
