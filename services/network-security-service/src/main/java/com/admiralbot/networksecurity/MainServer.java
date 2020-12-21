package com.admiralbot.networksecurity;

import com.admiralbot.framework.server.HttpApiServer;
import com.admiralbot.networksecurity.model.INetworkSecurity;
import com.admiralbot.sharedutil.AppContext;

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
