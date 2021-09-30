package com.admiralbot.echoservice;

import com.admiralbot.echoservice.model.IEchoService;
import com.admiralbot.framework.server.LambdaProxyApiServer;
import com.admiralbot.sharedutil.AppContext;

public class MainServer extends LambdaProxyApiServer<IEchoService> {

    public static void main(String[] args) {
        AppContext.setLambda();
        new MainServer();
    }

    @Override
    protected Class<IEchoService> getModelClass() {
        return IEchoService.class;
    }

    @Override
    protected IEchoService createHandlerInstance() {
        return new EchoServiceHandler();
    }
}
