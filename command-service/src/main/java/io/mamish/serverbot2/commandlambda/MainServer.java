package io.mamish.serverbot2.commandlambda;

import io.mamish.serverbot2.commandlambda.model.ICommandService;
import io.mamish.serverbot2.framework.server.HttpApiServer;
import io.mamish.serverbot2.sharedutil.AppContext;

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
