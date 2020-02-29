package box;

import java.io.ByteArrayOutputStream;

/**
 * Created by wangkang on 19/7/17.
 */
public class StringReceivePacket extends AbsByteArrayReceivePacket<String> {



    public StringReceivePacket(long leng) {
        super(leng);
    }


    @Override
    protected String buildEntity(ByteArrayOutputStream stream) {
        return new String(stream.toByteArray());
    }

    @Override
    public byte type() {
        return TYPE_MEMORY_STRING;
    }
}
