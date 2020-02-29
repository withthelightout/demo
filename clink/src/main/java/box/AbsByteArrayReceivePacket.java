package box;

import core.ReceivePacket;

import java.io.ByteArrayOutputStream;

/**
 * Created by wangkang on 19/7/23.
 */
public abstract class AbsByteArrayReceivePacket<Entity> extends ReceivePacket<ByteArrayOutputStream,Entity> {


    public AbsByteArrayReceivePacket(long len){
        super(len);
    }


    @Override
    protected final ByteArrayOutputStream createStream() {
        return new ByteArrayOutputStream((int) length);
    }
}
