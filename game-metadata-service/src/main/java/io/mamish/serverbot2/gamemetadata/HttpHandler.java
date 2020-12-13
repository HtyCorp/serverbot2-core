package io.mamish.serverbot2.gamemetadata;

import io.mamish.serverbot2.framework.server.HttpApiServer;
import io.mamish.serverbot2.gamemetadata.model.IGameMetadataService;

public class HttpHandler extends HttpApiServer<IGameMetadataService> {

    @Override
    protected Class<IGameMetadataService> getModelClass() {
        return IGameMetadataService.class;
    }

    @Override
    protected IGameMetadataService createHandlerInstance() {
        return new GameMetadataServiceHandler();
    }

}
