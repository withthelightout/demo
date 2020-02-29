import constants.Foo;
import constants.FooGui;
import constants.TCPConstants;
import core.IoContext;
import impl.IoStealingSelectorProvider;
import impl.SchedulerImpl;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by wangkang on 19/7/7. 完整版
 */
@EnableAutoConfiguration
@ComponentScan
public class Server {
    public static void main(String[] args) throws IOException {
        File cachePath = Foo.getCacheDir("server");
        IoContext.setup().ioProvider(new IoStealingSelectorProvider(4)).sheduler(new SchedulerImpl(1)).start();
        TCPServer tcpServer = new TCPServer(TCPConstants.PORT_SERVER, cachePath);
        boolean isSucceed = tcpServer.start();
        if (!isSucceed) {
            System.out.println("Start TCP server failed!");
            return;
        }
        UDPProvider.start(TCPConstants.PORT_SERVER);

        FooGui gui = new FooGui("Clink-Server", new FooGui.Callback() {
            @Override
            public long[] takeText() {
                return tcpServer.getStatusString();
            }
        });
        gui.doShow();

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        String str;
        do {
            str = bufferedReader.readLine();
            if (str == null || Foo.COMMAND_EXIT.equalsIgnoreCase(str)) {
                break;
            }
            if (str.length() == 0) {
                continue;
            }
            tcpServer.broadcast(str);
        } while (true);
        UDPProvider.stop();
        tcpServer.stop();
    }


}
