package com.admiralbot.urlshortener;

import com.admiralbot.framework.server.HttpApiServer;
import com.admiralbot.sharedutil.AppContext;
import com.admiralbot.urlshortener.model.IUrlShortener;

public class MainServer extends HttpApiServer<IUrlShortener> {

    public static void main(String[] args) {
        AppContext.setContainer();
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
