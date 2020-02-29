import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

import java.nio.ByteBuffer;

@EnableAutoConfiguration
@ComponentScan
public class Main {
    public static void main(String[] args) {
        ByteBuffer buffer = ByteBuffer.allocate(256);
        buffer.clear();
        System.out.println("ok");
    }
}
