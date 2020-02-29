package frames;

import core.Frame;
import core.IoArgs;

import java.io.IOException;

/**
 * Created by wangkang on 19/7/27.
 */
public class HeartbeatSendFrame extends AbsSendFrame {
    static final byte[] HEARTBEAT_DATE = new byte[]{0, 0, Frame.TYPE_COMMAND_HEARTBEAT, 0, 0, 0};

    public HeartbeatSendFrame() {
        super(HEARTBEAT_DATE);
    }

    @Override
    protected int consumeBody(IoArgs ioArgs) throws IOException {
        return 0;
    }
}
