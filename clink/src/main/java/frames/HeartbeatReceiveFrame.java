package frames;

import core.IoArgs;

import java.io.IOException;

/**
 * Created by wangkang on 19/7/28.
 */
public class HeartbeatReceiveFrame extends AbsReceiveFrame {
    static final HeartbeatReceiveFrame INSTANCE = new HeartbeatReceiveFrame();

    private HeartbeatReceiveFrame() {
        super(HeartbeatSendFrame.HEARTBEAT_DATE);
    }

    @Override
    protected int consumeBody(IoArgs args) throws IOException {
        return 0;
    }
}
