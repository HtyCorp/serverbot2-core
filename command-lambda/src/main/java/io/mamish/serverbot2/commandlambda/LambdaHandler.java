package io.mamish.serverbot2.commandlambda;

import io.mamish.serverbot2.commandlambda.model.service.ICommandService;
import io.mamish.serverbot2.commandlambda.model.service.ProcessUserCommandRequest;
import io.mamish.serverbot2.commandlambda.model.service.ProcessUserCommandResponse;
import io.mamish.serverbot2.framework.server.LambdaApiServer;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.StateMachineListItem;

import java.util.Map;
import java.util.stream.Collectors;

public class LambdaHandler extends LambdaApiServer<ICommandService> implements ICommandService {

    private final ServersCommandHandler serversCommandHandler;
    private final WelcomeCommandHandler welcomeCommandHandler;

    public LambdaHandler() {
        SfnClient sfnClient = SfnClient.create();
        Map<String,String> stateMachineArns = sfnClient.listStateMachines().stateMachines().stream().collect(Collectors.toMap(
                StateMachineListItem::name,
                StateMachineListItem::stateMachineArn
        ));
        serversCommandHandler = new ServersCommandHandler(sfnClient, stateMachineArns);
        welcomeCommandHandler = new WelcomeCommandHandler();

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
        return serversCommandHandler.handleRequest(commandRequest);
    }
}
