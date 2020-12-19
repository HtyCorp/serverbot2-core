package io.mamish.serverbot2.networksecurity;

import io.mamish.serverbot2.framework.server.HttpApiServer;
import io.mamish.serverbot2.networksecurity.model.INetworkSecurity;
import io.mamish.serverbot2.sharedutil.AppContext;

public class MainServer extends HttpApiServer<INetworkSecurity> {

    public static void main(String[] args) {
        AppContext.setContainer();
        new MainServer();
    }

    @Override
    protected Class<INetworkSecurity> getModelClass() {
        return INetworkSecurity.class;
    }

    @Override
    protected INetworkSecurity createHandlerInstance() {
        return new NetworkSecurityServiceHandler();
    }

}
