package impl.stealing;

import core.IoProvider;

import java.nio.channels.SocketChannel;

/**
 * Created by wangkang on 19/7/31.
 */
public class IoTask {
    public final SocketChannel channel;
    public final IoProvider.HandleProviderCallback providerCallback;
    public final int ops;


    public IoTask(SocketChannel channel, int ops, IoProvider.HandleProviderCallback providerCallback) {
        this.channel = channel;
        this.providerCallback = providerCallback;
        this.ops = ops;
    }


}
