import Utils.CloseUtils;
import bean.ServerInfo;
import box.FileSendPacket;
import constants.Foo;
import core.Connector;
import core.IoContext;
import core.SchedulerJob;
import core.schedule.IdleTimeoutScheduleJob;
import handle.ConnectorCloseChain;
import handle.ConnectorHandler;
import impl.IoStealingSelectorProvider;
import impl.SchedulerImpl;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

import java.io.*;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

/**
 * Created by wangkang on 19/7/11.
 */
@EnableAutoConfiguration
@ComponentScan
public class Client {

    public static void main(String[] args) throws IOException {
        File cachePath = Foo.getCacheDir("client");
        IoContext.setup().ioProvider(new IoStealingSelectorProvider(1)).sheduler(new SchedulerImpl(1)).start();
        ServerInfo info = UDPSearcher.searchServer(10000);
        System.out.println("Server:" + info);

        if (info != null) {
            TCPClient tcpClient = null;

            try {
                tcpClient = TCPClient.startWith(info, cachePath);
                if (tcpClient == null) {
                    return;
                }
                tcpClient.getCloseChain().appendLast(new ConnectorCloseChain() {
                    @Override
                    protected boolean consume(ConnectorHandler clientHandler, Connector connector) {
                        CloseUtils.close(System.in);
                        return true;
                    }
                });
                SchedulerJob schedulerJob = new IdleTimeoutScheduleJob(10, TimeUnit.HOURS, tcpClient);
                tcpClient.schedule(schedulerJob);

                write(tcpClient);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (tcpClient != null) {
                    tcpClient.exit();
                }
            }
        }
        IoContext.close();
    }

    private static void write(TCPClient tcpClient) throws IOException {
        // 构建键盘输入流
        InputStream in = System.in;
        BufferedReader input = new BufferedReader(new InputStreamReader(in));

        do {
            // 键盘读取一行
            String str = input.readLine();
            if (str == null || Foo.COMMAND_EXIT.equalsIgnoreCase(str)) {
                break;
            }
            if (str.length() == 0) {
                continue;
            }
            if (str.startsWith("--f")) {
                String[] array = str.split(" ");
                if (array.length >= 2) {
                    String filePath = array[1];
                    File file = new File(filePath);
                    if (file.exists() && file.isFile()) {
                        FileSendPacket fileSendPacket = new FileSendPacket(file);
                        tcpClient.send(fileSendPacket);

                        continue;
                    }
                }
            }
            // 发送到服务器
            tcpClient.send(str);
        } while (true);

          /*
          ServerSocketChannel server = ServerSocketChannel.open();
          server.configureBlocking(false);
          server.socket().bind(port);
          server.register(select,事件)
          selector.select()
          SelectionKey  = select.selectKeys();
          ServerSocketChannel server = SelectionKey.channel();
          SocketChannel socketChannel = server.accept();


          String msg = "content";
          ReadableByteChannel readChannel = Channels.newChannel(new ByteArrayInputStream(msg.getBytes()));
          ByteBuffer byteBuffer = ByteBuffer.allocate(128);
          readChannel.read(buffer); channel --->buffer
          WritableByteChannel writerChannel = Channels.newChannel(new ByteArrayOutputStream((int) length));
          writerChannel.write(buffer); buffer-->channel
          String msg = new String(stream.toByteArray());

          new FileOutputStream(file);
          new FileInputStream(file);
         */

    }
}
