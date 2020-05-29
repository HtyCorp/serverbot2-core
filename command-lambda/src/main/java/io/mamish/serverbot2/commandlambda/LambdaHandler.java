package io.mamish.serverbot2.commandlambda;

import io.mamish.serverbot2.commandlambda.model.service.ICommandService;
import io.mamish.serverbot2.commandlambda.model.service.ProcessUserCommandRequest;
import io.mamish.serverbot2.commandlambda.model.service.ProcessUserCommandResponse;
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
    public ProcessUserCommandResponse processUserCommand(ProcessUserCommandRequest commandRequest) {
        return commandHandler.handleRequest(commandRequest);
    }
}
