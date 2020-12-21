package com.admiralbot.gamemetadata;

import com.admiralbot.framework.server.HttpApiServer;
import com.admiralbot.gamemetadata.model.IGameMetadataService;
import com.admiralbot.sharedutil.AppContext;

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
