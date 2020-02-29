package handle;

import box.StringReceivePacket;

/**
 * Created by wangkang on 19/7/29.
 */
public class DefaultNonConnectorStringPacketChain extends ConnectorStringPacketChain {
    @Override
    protected boolean consume(ConnectorHandler clientHandler, StringReceivePacket stringReceivePacket) {
        return false;
    }
}
