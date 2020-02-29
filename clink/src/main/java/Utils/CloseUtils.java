package Utils;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by wangkang on 19/7/7.
 */
public class CloseUtils {

    public static void close(Closeable... closeables) {
        if (closeables == null) {
            return;
        }
        for (Closeable closeable : closeables) {
            if (closeable == null) continue;
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
