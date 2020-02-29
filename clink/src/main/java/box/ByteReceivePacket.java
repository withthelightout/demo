package box;

import java.io.ByteArrayOutputStream;

/**
 * Created by wangkang on 19/7/23.
 */
public class ByteReceivePacket extends AbsByteArrayReceivePacket<byte[]> {
    public ByteReceivePacket(long len) {
        super(len);
    }

    @Override
    public byte type() {
        return TYPE_MEMORY_BYTES;
    }

    @Override
    protected byte[] buildEntity(ByteArrayOutputStream stream) {
        return stream.toByteArray();
    }


}
