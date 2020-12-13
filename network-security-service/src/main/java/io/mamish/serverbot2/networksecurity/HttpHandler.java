package io.mamish.serverbot2.networksecurity;

import io.mamish.serverbot2.framework.server.HttpApiServer;
import io.mamish.serverbot2.networksecurity.model.INetworkSecurity;

public class HttpHandler extends HttpApiServer<INetworkSecurity> {

    @Override
    protected Class<INetworkSecurity> getModelClass() {
        return INetworkSecurity.class;
    }

    @Override
    protected INetworkSecurity createHandlerInstance() {
        return new NetworkSecurityServiceHandler();
    }

}
