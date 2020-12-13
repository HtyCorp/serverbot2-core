package io.mamish.serverbot2.commandlambda;

import io.mamish.serverbot2.commandlambda.model.ICommandService;
import io.mamish.serverbot2.framework.server.HttpApiServer;

public class HttpHandler extends HttpApiServer<ICommandService> {

    @Override
    protected Class<ICommandService> getModelClass() {
        return ICommandService.class;
    }

    @Override
    protected ICommandService createHandlerInstance() {
        return new RootCommandHandler();
    }

}
