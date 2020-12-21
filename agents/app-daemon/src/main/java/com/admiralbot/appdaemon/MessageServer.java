package com.admiralbot.appdaemon;

import com.admiralbot.appdaemon.model.IAppDaemon;
import com.admiralbot.framework.server.SqsApiServer;

public class MessageServer extends SqsApiServer<IAppDaemon> {

    public MessageServer() {
        super(GameMetadataFetcher.initial().getInstanceQueueName());
    }

    @Override
    protected Class<IAppDaemon> getModelClass() {
        return IAppDaemon.class;
    }

    @Override
    protected IAppDaemon createHandlerInstance() {
        return new ServiceHandler();
    }
}
