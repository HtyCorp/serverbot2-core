package io.mamish.serverbot2.commandlambda;

import io.mamish.serverbot2.commandlambda.model.service.ICommandService;
import io.mamish.serverbot2.commandlambda.model.service.ProcessUserCommandRequest;
import io.mamish.serverbot2.commandlambda.model.service.ProcessUserCommandResponse;
import io.mamish.serverbot2.discordrelay.model.service.MessageChannel;
import io.mamish.serverbot2.framework.exception.server.RequestHandlingRuntimeException;

public class RootCommandHandler implements ICommandService {

    private final AdminCommandHandler adminCommandHandler;
    private final ServersCommandHandler serversCommandHandler;
    private final WelcomeCommandHandler welcomeCommandHandler;

    public RootCommandHandler() {
        SfnRunner sfnRunner = new SfnRunner();
        adminCommandHandler = new AdminCommandHandler(sfnRunner);
        serversCommandHandler = new ServersCommandHandler(sfnRunner);
        welcomeCommandHandler = new WelcomeCommandHandler();

        // Chaining: admin -> debug (pending) -> servers -> welcome
        // Commands from lower in chain can be used implicitly from a higher-level channel.
        adminCommandHandler.setNextChainHandler(serversCommandHandler);
        serversCommandHandler.setNextChainHandler(welcomeCommandHandler);
    }

    @Override
    public ProcessUserCommandResponse processUserCommand(ProcessUserCommandRequest commandRequest) {
        // Passes the request to the appropriate handler based on source channel.
        // Note that the chaining mechanism will pass requests downwards if not found in source channel.
        MessageChannel channel = commandRequest.getChannel();
        if (channel == MessageChannel.ADMIN) {
            return adminCommandHandler.handleRequest(commandRequest);
        } else if (channel == MessageChannel.DEBUG) {
            // There are no commands yet, and Discord relay is configured not to relay from this channel.
            return new ProcessUserCommandResponse(null, null);
        } else if (channel == MessageChannel.SERVERS) {
            return serversCommandHandler.handleRequest(commandRequest);
        } else if (channel == MessageChannel.WELCOME) {
            return welcomeCommandHandler.handleRequest(commandRequest);
        } else {
            throw new RequestHandlingRuntimeException("Impossible: received an unknown channel");
        }
    }
}
