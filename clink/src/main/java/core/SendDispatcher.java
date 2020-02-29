package core;

import java.io.Closeable;

/**
 * Created by wangkang on 19/7/17.
 */
public interface SendDispatcher extends Closeable{

    void send(SendPacket sendPacket);

    void cancel(SendPacket sendPacket);

    void sendHeartbeat();
}
