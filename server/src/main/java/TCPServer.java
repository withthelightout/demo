import Utils.CloseUtils;
import box.StringReceivePacket;
import constants.Foo;
import core.Connector;
import core.SchedulerJob;
import core.schedule.IdleTimeoutScheduleJob;
import handle.ConnectorCloseChain;
import handle.ConnectorHandler;
import handle.ConnectorStringPacketChain;
import handle.PrintStringPacketChain;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by wangkang on 19/7/7.
 */
public class TCPServer implements ServerAcceptor.AcceptListener, Group.GroupMessageAdapter {
    private final int port;
    private final File cachePath;
    private final Map<String, Group> groups = new HashMap<>();
    private final List<ConnectorHandler> clientHandlerList = new ArrayList<>();
    private ServerAcceptor acceptor;
    private Selector selector;
    private ServerSocketChannel server;
    public static long receiveSize;
    public static long sendSize;

    public TCPServer(int port, File file) {
        this.cachePath = file;
        this.port = port;
        this.groups.put(Foo.DEFAULT_GROUP_NAME, new Group(Foo.DEFAULT_GROUP_NAME, this));
    }

    public boolean start() {
        try {
            ServerAcceptor acceptor = new ServerAcceptor(this);
            ServerSocketChannel server = ServerSocketChannel.open();
            server.configureBlocking(false);
            server.socket().bind(new InetSocketAddress(port));

            server.register(acceptor.getSelector(), SelectionKey.OP_ACCEPT);
            this.server = server;
            this.acceptor = acceptor;
            acceptor.start();
            if (acceptor.awaitRunning()) {
                System.out.println("服务器准备就绪");
                System.out.println("服务器信息：" + server.getLocalAddress().toString());
                return true;
            } else {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    void stop() {
        if (acceptor != null) {
            acceptor.exit();
        }
        ConnectorHandler[] clientHandlers;
        synchronized (clientHandlerList) {
            clientHandlers = clientHandlerList.toArray(new ConnectorHandler[0]);
            clientHandlerList.clear();
        }
        for (ConnectorHandler clientHander : clientHandlers) {
            clientHander.exit();
        }


        CloseUtils.close(server);

    }

    void broadcast(String str) {
        str += "系统通知";

        ConnectorHandler[] clientHandlers;
        synchronized (clientHandlerList) {
            clientHandlers = clientHandlerList.toArray(new ConnectorHandler[0]);

        }

        for (ConnectorHandler clientHandler : clientHandlers) {
            sendMessageToClient(clientHandler, str);
        }


    }

    @Override
    public void sendMessageToClient(ConnectorHandler handler, String msg) {
        sendSize++;
        receiveSize++;
        handler.send(msg);
    }

    long[] getStatusString() {
        return new long[]{
                clientHandlerList.size(),
                sendSize,
                receiveSize,
        };
    }


    private class RemoveQueueOnConnectorClosedChain extends ConnectorCloseChain {

        @Override
        protected boolean consume(ConnectorHandler clientHandler, Connector connector) {
            synchronized (clientHandler) {
                clientHandlerList.remove(clientHandler);
                Group group = groups.get(Foo.DEFAULT_GROUP_NAME);
                group.removeMember(clientHandler);
            }

            return true;
        }
    }


    @Override
    public void onNewSocketArrived(SocketChannel socketChannel) {
        try {
            ConnectorHandler clientHandler = new ConnectorHandler(socketChannel, cachePath);
            System.out.println(clientHandler.getClientInfo() + ": Connect");
            clientHandler.getCloseChain()
                    .appendLast(new RemoveQueueOnConnectorClosedChain());
            clientHandler.getStringPacketChain().appendLast(new ParseCommandConnectorStringPacketChain()).appendLast(new PrintStringPacketChain()).appendLast(new MessageReturnStringPacketChain());

            SchedulerJob schedulerJob = new IdleTimeoutScheduleJob(10, TimeUnit.HOURS, clientHandler);
            clientHandler.schedule(schedulerJob);
            synchronized (clientHandlerList) {
                clientHandlerList.add(clientHandler);
                System.out.println("当前客户端数量" + clientHandlerList.size());
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("客户端连接异常" + e.getMessage());
        }
    }

    private class MessageReturnStringPacketChain extends ConnectorStringPacketChain{

        @Override
        protected boolean consume(ConnectorHandler clientHandler, StringReceivePacket stringReceivePacket) {
            sendMessageToClient(clientHandler, stringReceivePacket.entity());
            return true;
        }
    }

    private class ParseCommandConnectorStringPacketChain extends ConnectorStringPacketChain {

        @Override
        protected boolean consume(ConnectorHandler clientHandler, StringReceivePacket stringReceivePacket) {
            String str = stringReceivePacket.entity();
            if (str.startsWith(Foo.COMMAND_GROUP_JOIN)) {
                Group group = groups.get(Foo.DEFAULT_GROUP_NAME);
                if (group.addMember(clientHandler)) {
                    sendMessageToClient(clientHandler, "Join Group:" + group.getName());
                }
                return true;
            } else if (str.startsWith(Foo.COMMAND_GROUP_LEAVE)) {
                Group group = groups.get(Foo.DEFAULT_GROUP_NAME);
                if (group.removeMember(clientHandler)) {
                    sendMessageToClient(clientHandler, "Leave Group:" + group.getName());
                }
                return true;
            }
            return false;
        }

        @Override
        protected boolean consumeAgain(ConnectorHandler handler, StringReceivePacket stringReceivePacket) {
            sendMessageToClient(handler, stringReceivePacket.entity());
            return true;
        }
    }
}



