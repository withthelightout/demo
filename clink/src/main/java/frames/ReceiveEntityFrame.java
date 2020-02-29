package frames;

import core.IoArgs;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

/**
 * Created by wangkang on 19/7/27.
 */
public class ReceiveEntityFrame extends AbsReceiveFrame {

    private WritableByteChannel channel;

    ReceiveEntityFrame(byte[] header) {
        super(header);
    }

    public void bindPacketChannel(WritableByteChannel writableByteChannel) {
        this.channel = writableByteChannel;
    }

    @Override
    protected int consumeBody(IoArgs ioArgs) throws IOException {
        return channel == null ? ioArgs.setEmpty(bodyRemaining) : ioArgs.writeTo(channel);
    }
}
