import Utils.CloseUtils;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;

/**
 * Created by wangkang on 19/7/29.
 */
public class ServerAcceptor extends Thread {
    private final AcceptListener listener;
    private boolean done = false;
    private final Selector selector;
    private final CountDownLatch latch = new CountDownLatch(1);

    ServerAcceptor(AcceptListener listener) throws IOException {
        super("Server-Accept-Thread");
        this.listener = listener;
        this.selector = Selector.open();
    }

    boolean awaitRunning() {
        try {
            latch.await();
            return true;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void run() {
        super.run();
        latch.countDown();
        Selector selector = this.selector;
        System.out.println("服务器准备就绪～");
        do {
            try {
                    if (selector.select() == 0) {
                    if (done) {
                        break;
                    }
                    continue;
                }
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    if (done) {
                        break;
                    }
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    if (key.isAcceptable()) {
                        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
                        SocketChannel socketChannel = serverSocketChannel.accept();
                        listener.onNewSocketArrived(socketChannel);
                    }
                }
            } catch (IOException e) {
                continue;
            }
        } while (!done);
        System.out.println("ServerAcceptor Finish");
    }

    void exit() {
        done = true;
        CloseUtils.close(selector);
    }

    interface AcceptListener {
        void onNewSocketArrived(SocketChannel socketChannel);
    }

    public Selector getSelector() {
        return selector;
    }
}
