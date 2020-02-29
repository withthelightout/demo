package core;

import java.io.IOException;

/**
 * Created by wangkang on 19/7/27.
 */
public abstract class Frame {
    public static final int FRAME_HEADER_LENGTH = 6;
    public static final int MAX_CAPACITY = 64 * 1024 - 1;
    protected final byte[] header = new byte[FRAME_HEADER_LENGTH];

    // Packet头信息帧
    public static final byte TYPE_PACKET_HEADER = 11;
    // Packet数据分片信息帧
    public static final byte TYPE_PACKET_ENTITY = 12;
    // 指令-发送取消
    public static final byte TYPE_COMMAND_SEND_CANCEL = 41;
    // 指令-接受拒绝
    public static final byte TYPE_COMMAND_RECEIVE_REJECT = 42;

    public static final byte TYPE_COMMAND_HEARTBEAT = 81;

    // Flag标记
    public static final byte FLAG_NONE = 0;

    public Frame(byte[] header) {
        System.arraycopy(header, 0, this.header, 0, FRAME_HEADER_LENGTH);
    }


    public Frame(int length, byte type, byte flag, short identifer) {
        if (length < 0 || length > MAX_CAPACITY) {
            throw new RuntimeException("");
        }
        if (identifer < 0 || identifer > 255) {
            throw new RuntimeException("");
        }
        header[0] = (byte) (length >> 8);
        header[1] = (byte) (length);
        header[2] = type;
        header[3] = flag;
        header[4] = (byte) identifer;
        header[5] = 0;

    }

    public int getBodyLength() {
        return ((((int) header[0]) & 0xFF) << 8) | (((int) header[1]) & 0xFF);
    }

    public byte getBodyType() {
        return header[2];
    }

    /**
     * 获取Body的Flag
     *
     * @return Flag
     */
    public byte getBodyFlag() {
        return header[3];
    }

    public short getBodyIdentifier() {
        return (short) ((short) header[4] & 0xFF);
    }

    public abstract boolean handle(IoArgs ioArgs) throws IOException;

    public abstract Frame nextFrame();


    public abstract int getConsumableLength();

}
