package io.mamish.serverbot2.gamemetadata;

import io.mamish.serverbot2.framework.server.LambdaApiServer;
import io.mamish.serverbot2.gamemetadata.model.IGameMetadataService;

public class LambdaHandler extends LambdaApiServer<IGameMetadataService> {

    private GameMetadataServiceHandler serviceHandler = new GameMetadataServiceHandler();

    @Override
    protected Class<IGameMetadataService> getModelClass() {
        return IGameMetadataService.class;
    }

    @Override
    protected IGameMetadataService getHandlerInstance() {
        return serviceHandler;
    }

}
