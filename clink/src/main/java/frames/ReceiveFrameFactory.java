package frames;


import core.Frame;
import core.IoArgs;

/**
 * Created by wangkang on 19/7/28.
 */
public class ReceiveFrameFactory {

    public static AbsReceiveFrame createInstance(IoArgs args) {
        byte[] buffer = new byte[Frame.FRAME_HEADER_LENGTH];
        args.writeTo(buffer, 0);
        byte type = buffer[2];
        switch (type) {
            case Frame.TYPE_COMMAND_SEND_CANCEL:
                return new CancelReceiveFrame(buffer);
            case Frame.TYPE_PACKET_HEADER:
                return new ReceiveHeaderFrame(buffer);
            case Frame.TYPE_PACKET_ENTITY:
                return new ReceiveEntityFrame(buffer);
            case Frame.TYPE_COMMAND_HEARTBEAT:
                return HeartbeatReceiveFrame.INSTANCE;
            default:
                throw new UnsupportedOperationException("Unsupported frame type:" + type);
        }
    }
}
