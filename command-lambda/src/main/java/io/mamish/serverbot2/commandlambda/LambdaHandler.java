package io.mamish.serverbot2.commandlambda;

import io.mamish.serverbot2.commandlambda.model.service.ICommandService;
import io.mamish.serverbot2.framework.server.LambdaApiServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LambdaHandler extends LambdaApiServer<ICommandService> {

    @Override
    protected Class<ICommandService> getModelClass() {
        return ICommandService.class;
    }

    @Override
    protected ICommandService createHandlerInstance() {
        return new RootCommandHandler();
    }

}
