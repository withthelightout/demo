import Utils.CloseUtils;
import bean.ServerInfo;
import constants.Foo;
import core.Connector;
import core.IoContext;
import handle.ConnectorCloseChain;
import handle.ConnectorHandler;
import impl.IoStealingSelectorProvider;
import impl.SchedulerImpl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClientTest {
    private static boolean done;
    private static final int CLIENT_SIZE = 2000;
    private static final int SEND_THREAD_SIZE = 4;
    private static final int SEND_THREAD_DELAY = 200;

    public static void main(String[] args) throws IOException, InterruptedException {

        ServerInfo info = UDPSearcher.searchServer(10000);
        System.out.println("Server:" + info);
        if (info == null) return;

        File cachePath = Foo.getCacheDir("client/test");
        IoContext.setup().ioProvider(new IoStealingSelectorProvider(3)).sheduler(new SchedulerImpl(1)).start();


        // 当前连接数量
        int size = 0;
        final List<TCPClient> tcpClients = new ArrayList<>(CLIENT_SIZE);
        final ConnectorCloseChain closeChain = new ConnectorCloseChain() {
            @Override
            protected boolean consume(ConnectorHandler clientHandler, Connector connector) {
                tcpClients.remove(clientHandler);
                if (tcpClients.size() == 0) {
                    CloseUtils.close(System.in);
                }
                return false;
            }
        };
        for (int i = 0; i < CLIENT_SIZE; i++) {
            try {
                TCPClient tcpClient = TCPClient.startWith(info, cachePath, false);
                if (tcpClient == null) {
                    throw new NullPointerException();
                }
                tcpClient.getCloseChain().appendLast(closeChain);
                tcpClients.add(tcpClient);

                System.out.println("连接成功：" + (++size));

            } catch (IOException | NullPointerException e) {
                System.out.println("连接异常");
                e.printStackTrace();
                break;
            }

        }

        System.in.read();

        Runnable runnable = () -> {
            while (!done) {
                TCPClient[] copyClient = tcpClients.toArray(new TCPClient[0]);
                for (TCPClient client : copyClient) {
                    client.send("Hello~");
                }
                if (SEND_THREAD_DELAY > 0) {
                    try {
                        Thread.sleep(SEND_THREAD_DELAY);
                    } catch (InterruptedException e) {

                    }
                }

            }
        };

        List<Thread> threads = new ArrayList<>(SEND_THREAD_SIZE);
        for (int i = 0; i < SEND_THREAD_SIZE; i++) {
            Thread thread = new Thread(runnable);
            thread.start();
            threads.add(thread);
        }

        Thread.sleep(1000000);


        // 等待线程完成
        done = true;

        // 客户端结束操作
        for (TCPClient tcpClient : tcpClients) {
            tcpClient.exit();
        }
        IoContext.close();

        for (Thread thread : threads) {
            thread.interrupt();
        }

    }


}
