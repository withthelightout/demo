package box;

import core.SendPacket;

import java.io.ByteArrayInputStream;

/**
 * Created by wangkang on 19/7/23.
 */
public class ByteSendPacket extends SendPacket<ByteArrayInputStream> {

    private final byte[] bytes;

    public ByteSendPacket(byte[] bytes) {
        this.bytes = bytes;
        this.length = bytes.length;
    }

    @Override
    public byte type(){
        return TYPE_MEMORY_BYTES;
    }

    @Override
    protected ByteArrayInputStream createStream() {
        return new ByteArrayInputStream(bytes);
    }
}
