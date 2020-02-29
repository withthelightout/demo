package handle;

import core.Connector;

/**
 * Created by wangkang on 19/7/29.
 */
public class DefaultPrintConnectorCloseChain extends ConnectorCloseChain {
    @Override
    protected boolean consume(ConnectorHandler clientHandler, Connector connector) {
        System.out.println(clientHandler.getClientInfo()+"Exit key"+clientHandler.getKey().toString());
        return false;
    }
}
