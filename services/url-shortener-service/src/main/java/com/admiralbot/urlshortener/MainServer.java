package com.admiralbot.urlshortener;

import com.admiralbot.framework.server.LambdaProxyApiServer;
import com.admiralbot.sharedutil.AppContext;
import com.admiralbot.urlshortener.model.IUrlShortener;

public class MainServer extends LambdaProxyApiServer<IUrlShortener> {

    public static void main(String[] args) {
        AppContext.setLambda();
        new MainServer();
    }

    @Override
    protected Class<IUrlShortener> getModelClass() {
        return IUrlShortener.class;
    }

    @Override
    protected IUrlShortener createHandlerInstance() {
        return new ServiceHandler();
    }

}
