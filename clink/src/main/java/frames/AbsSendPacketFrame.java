package frames;

import core.Frame;
import core.IoArgs;
import core.SendPacket;

import java.io.IOException;

/**
 * Created by wangkang on 19/7/28.
 */
public abstract class AbsSendPacketFrame extends AbsSendFrame {




    public synchronized SendPacket getPacket() {
        return packet;
    }

    protected volatile SendPacket<?> packet;

    public AbsSendPacketFrame(int length, byte type, byte flag, short identifier, SendPacket packet) {
        super(length, type, flag, identifier);
        this.packet = packet;
    }

    @Override
    public boolean handle(IoArgs ioArgs) throws IOException {
        if (packet == null && !isSending()) {
            return true;
        }
        return super.handle(ioArgs);
    }

    public final synchronized boolean abort() {
        boolean isSending = isSending();
        if (isSending) {
            fillDirtyDataObAbort();
        }
        packet = null;
        return !isSending;
    }

    private void fillDirtyDataObAbort() {
    }

    @Override
    public final synchronized Frame nextFrame() {
        return packet == null ? null : buildNextFrame();
    }

    protected abstract Frame buildNextFrame();
}
