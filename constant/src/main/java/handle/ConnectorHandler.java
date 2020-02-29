package handle;

import Utils.CloseUtils;
import box.StringReceivePacket;
import constants.Foo;
import core.Connector;
import core.IoContext;
import core.Packet;
import core.ReceivePacket;

import java.io.File;
import java.io.IOException;
import java.nio.channels.SocketChannel;


/**
 * Created by wangkang on 19/7/7.
 */
public class ConnectorHandler extends Connector {
    private final File cachePath;
    private final SocketChannel socketChannel;

    private final String clientInfo;
    private final ConnectorCloseChain closeChain = new DefaultPrintConnectorCloseChain();
    private final ConnectorStringPacketChain stringPacketChain = new DefaultNonConnectorStringPacketChain();

    public ConnectorHandler(SocketChannel socketChannel, File cachePath) throws IOException {
        this.cachePath = cachePath;
        this.socketChannel = socketChannel;
        setup(socketChannel);


        this.clientInfo = socketChannel.getRemoteAddress().toString();
    }


    public void exit() {
        CloseUtils.close(this);
    }


    @Override
    protected File createNewReceiveFile() {
        return Foo.createRandomTemp(cachePath);
    }

    @Override
    public void onChannelClosed(SocketChannel socketChannel) {
        super.onChannelClosed(socketChannel);
        closeChain.handle(this, this);
    }


    public String getClientInfo() {
        return clientInfo;
    }


    @Override
    protected void onReceivedPacket(ReceivePacket packet) {
        super.onReceivedPacket(packet);
        switch (packet.type()) {
            case Packet.TYPE_MEMORY_STRING: {
                deliveryStringPacket((StringReceivePacket) packet);
                break;
            }
            default: {
                System.out.print("New Packet:" + packet.type() + "-" + packet.length());
            }
        }
    }

    private void deliveryStringPacket(StringReceivePacket packet) {
        IoContext.get().getScheduler().delivery(()->stringPacketChain.handle(this,packet));
    }


    public ConnectorCloseChain getCloseChain() {
        return closeChain;
    }

    public ConnectorStringPacketChain getStringPacketChain() {
        return stringPacketChain;
    }
}
