package io.mamish.serverbot2.commandlambda;

import io.mamish.serverbot2.commandlambda.model.service.ICommandService;
import io.mamish.serverbot2.commandlambda.model.service.ProcessUserCommandRequest;
import io.mamish.serverbot2.commandlambda.model.service.ProcessUserCommandResponse;
import io.mamish.serverbot2.framework.server.LambdaApiServer;

public class LambdaHandler extends LambdaApiServer<ICommandService> implements ICommandService {

    private final AdminCommandHandler adminCommandHandler;

    public LambdaHandler() {
        SfnRunner sfnRunner = new SfnRunner();
        adminCommandHandler = new AdminCommandHandler(sfnRunner);
        ServersCommandHandler serversCommandHandler = new ServersCommandHandler(sfnRunner);
        WelcomeCommandHandler welcomeCommandHandler = new WelcomeCommandHandler();

        // Chaining: admin -> debug (pending) -> servers -> welcome
        // Commands from lower in chain can be used implicitly from a higher-level channel.
        adminCommandHandler.setNextChainHandler(serversCommandHandler);
        serversCommandHandler.setNextChainHandler(welcomeCommandHandler);
    }

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
        // Will automatically be passed to the other chained handlers if case of an unknown request.
        return adminCommandHandler.handleRequest(commandRequest);
    }
}
