package frames;

import core.Frame;
import core.IoArgs;

import java.io.IOException;

/**
 * Created by wangkang on 19/7/27.
 */
public class CancelSendFrame extends AbsSendFrame {

    public CancelSendFrame(short identifer) {
        super(0, TYPE_COMMAND_SEND_CANCEL, Frame.FLAG_NONE, identifer);
    }

    @Override
    protected int consumeBody(IoArgs ioArgs) throws IOException {
        return 0;
    }
}
