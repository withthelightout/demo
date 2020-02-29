package frames;

import core.Frame;
import core.IoArgs;
import core.SendPacket;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;

/**
 * Created by wangkang on 19/7/27.
 */
public class SendEntityFrame extends AbsSendPacketFrame {
    private final ReadableByteChannel channel;
    //unConsumeEntityLength 全局未消费长度
    //bodyRemaining frame未消费长度
    private final long unConsumeEntityLength;

    public SendEntityFrame(short identifier, long entityLength, ReadableByteChannel channel, SendPacket packet) {
        super((int) Math.min(entityLength, Frame.MAX_CAPACITY), Frame.TYPE_PACKET_ENTITY, Frame.FLAG_NONE, identifier, packet);
        unConsumeEntityLength = entityLength - bodyRemaining;
        this.channel = channel;
    }

    @Override
    protected int consumeBody(IoArgs ioArgs) throws IOException {
        if(packet==null){
            return ioArgs.fillEmpty(bodyRemaining);
        }
        return ioArgs.readFrom(channel);
    }

    @Override
    public Frame buildNextFrame() {
        if (unConsumeEntityLength == 0) return null;
        return new SendEntityFrame(getBodyIdentifier(), unConsumeEntityLength, channel, packet);
    }
}
