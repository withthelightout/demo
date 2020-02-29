package box;

/**
 * Created by wangkang on 19/7/17.
 */
public class StringSendPacket extends ByteSendPacket {


    public StringSendPacket(String msg) {
       super(msg.getBytes());
    }


    @Override
    public byte type() {
        return TYPE_MEMORY_STRING;
    }

}
