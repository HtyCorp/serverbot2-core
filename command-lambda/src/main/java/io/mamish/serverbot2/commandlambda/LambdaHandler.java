package io.mamish.serverbot2.commandlambda;

import io.mamish.serverbot2.commandlambda.model.service.CommandServiceRequest;
import io.mamish.serverbot2.commandlambda.model.service.CommandServiceResponse;
import io.mamish.serverbot2.commandlambda.model.service.ICommandService;
import io.mamish.serverbot2.framework.server.LambdaApiServer;

public class LambdaHandler extends LambdaApiServer<ICommandService> implements ICommandService {

    private CommandHandler commandHandler = new CommandHandler();

    @Override
    protected Class<ICommandService> getModelClass() {
        return ICommandService.class;
    }

    @Override
    protected ICommandService getHandlerInstance() {
        return this;
    }

    @Override
    public CommandServiceResponse requestUserCommand(CommandServiceRequest commandServiceRequest) {
        return commandHandler.handleRequest(commandServiceRequest);
    }
}
