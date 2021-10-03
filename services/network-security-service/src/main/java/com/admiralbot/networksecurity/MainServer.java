package com.admiralbot.networksecurity;

import com.admiralbot.framework.server.LambdaProxyApiServer;
import com.admiralbot.networksecurity.model.INetworkSecurity;
import com.admiralbot.sharedutil.AppContext;

public class MainServer extends LambdaProxyApiServer<INetworkSecurity> {

    public static void main(String[] args) {
        AppContext.setLambda();
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
