package core;

import java.io.Closeable;

/**
 * Created by wangkang on 19/7/17.
 */
public interface ReceiveDispatcher extends Closeable{

    void start();

    void stop();

    interface ReceivePacketCallback{
        void onReceivePacketComplated(ReceivePacket packet);

        ReceivePacket<?,?> onArrivedNewPacket(byte type, long length);

        void onReceivedHeartbeat();
    }
}
