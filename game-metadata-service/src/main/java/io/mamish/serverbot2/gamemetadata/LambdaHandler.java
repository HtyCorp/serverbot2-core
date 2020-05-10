package io.mamish.serverbot2.gamemetadata;

import io.mamish.serverbot2.framework.server.LambdaStandardApiHandler;
import io.mamish.serverbot2.gamemetadata.model.IGameMetadataServiceHandler;

public class LambdaHandler extends LambdaStandardApiHandler<IGameMetadataServiceHandler> {

    private GameMetadataServiceHandler serviceHandler = new GameMetadataServiceHandler();

    @Override
    protected Class<IGameMetadataServiceHandler> getModelClass() {
        return IGameMetadataServiceHandler.class;
    }

    @Override
    protected IGameMetadataServiceHandler getHandlerInstance() {
        return serviceHandler;
    }

}
