package io.mamish.serverbot2.appdaemon;

import io.mamish.serverbot2.appdaemon.model.IAppDaemon;
import io.mamish.serverbot2.framework.server.SqsApiServer;

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
