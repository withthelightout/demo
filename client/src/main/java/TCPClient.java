import Utils.CloseUtils;
import bean.ServerInfo;
import handle.ConnectorHandler;
import handle.PrintStringPacketChain;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

/**
 * Created by wangkang on 19/7/11.
 */
public class TCPClient extends ConnectorHandler {


    public TCPClient(SocketChannel socketChannel, File cachePath,boolean printReceiveString) throws IOException {
        super(socketChannel, cachePath);
        if(printReceiveString){
            getStringPacketChain().appendLast(new PrintStringPacketChain());
        }
    }

//    private class PrintStringPacketChain extends ConnectorStringPacketChain {
//
//        @Override
//        protected boolean consume(ConnectorHandler clientHandler, StringReceivePacket stringReceivePacket) {
//            String str = stringReceivePacket.entity();
//            System.out.println(str);
//            return true;
//        }
//    }

    static TCPClient startWith(ServerInfo info, File cachePath)throws IOException{
        return startWith(info,cachePath,true);
    }


    static TCPClient startWith(ServerInfo info, File cachePath, boolean printReceiveString) throws IOException {
        SocketChannel socketchannel = SocketChannel.open();


        // 连接远程，端口2000；超时时间3000ms
        socketchannel.connect(new InetSocketAddress(Inet4Address.getByName(info.getAddress()), info.getPort()));

        System.out.println("已发起服务器连接，并进入后续流程～");
        System.out.println("客户端信息：" + socketchannel.getLocalAddress().toString());
        System.out.println("服务器信息：" + socketchannel.getRemoteAddress().toString());

        try {
            return new TCPClient(socketchannel, cachePath,printReceiveString);
        } catch (Exception e) {
            System.out.println("连接异常");
            CloseUtils.close(socketchannel);
        }

        return null;
    }

}
