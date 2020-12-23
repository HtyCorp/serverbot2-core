package com.admiralbot.commandservice;

import com.admiralbot.commandservice.handlers.RootCommandHandler;
import com.admiralbot.commandservice.model.ICommandService;
import com.admiralbot.framework.server.HttpApiServer;
import com.admiralbot.sharedutil.AppContext;

public class MainServer extends HttpApiServer<ICommandService> {

    public static void main(String[] args) {
        AppContext.setContainer();
        new MainServer();
    }

    @Override
    protected Class<ICommandService> getModelClass() {
        return ICommandService.class;
    }

    @Override
    protected ICommandService createHandlerInstance() {
        return new RootCommandHandler();
    }

}
