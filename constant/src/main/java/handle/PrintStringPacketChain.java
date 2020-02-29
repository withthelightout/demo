package handle;

import box.StringReceivePacket;


public class PrintStringPacketChain extends ConnectorStringPacketChain {

    @Override
    protected boolean consume(ConnectorHandler clientHandler, StringReceivePacket stringReceivePacket) {
        String str = stringReceivePacket.entity();
        System.out.println(str);
        return false;
    }
}
