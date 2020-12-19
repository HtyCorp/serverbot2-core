package io.mamish.serverbot2.gamemetadata;

import io.mamish.serverbot2.framework.server.HttpApiServer;
import io.mamish.serverbot2.gamemetadata.model.IGameMetadataService;
import io.mamish.serverbot2.sharedutil.AppContext;
import io.mamish.serverbot2.sharedutil.XrayUtils;

public class MainServer extends HttpApiServer<IGameMetadataService> {

    public static void main(String[] args) {
        AppContext.setContainer();
        new MainServer();
    }

    @Override
    protected Class<IGameMetadataService> getModelClass() {
        return IGameMetadataService.class;
    }

    @Override
    protected IGameMetadataService createHandlerInstance() {
        return new GameMetadataServiceHandler();
    }

}
