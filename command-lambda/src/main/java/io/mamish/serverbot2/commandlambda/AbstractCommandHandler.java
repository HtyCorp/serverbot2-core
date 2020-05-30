package io.mamish.serverbot2.commandlambda;

import io.mamish.serverbot2.commandlambda.model.commands.common.CommandHelp;
import io.mamish.serverbot2.commandlambda.model.service.ProcessUserCommandRequest;
import io.mamish.serverbot2.commandlambda.model.service.ProcessUserCommandResponse;

public abstract class AbstractCommandHandler<ModelType> {

    private final CommandDispatcher<ModelType> commandDispatcher;
    private final HelpHandler helpHandler;

    protected abstract Class<ModelType> getHandlerType();
    protected abstract ModelType getHandlerInstance();

    public AbstractCommandHandler() {
        commandDispatcher = new CommandDispatcher<>(getHandlerInstance(), getHandlerType());
        helpHandler = new HelpHandler(commandDispatcher.getApiDefinitionSet());
    }

    public void setNextChainHandler(AbstractCommandHandler<?> nextChainHandler) {
        commandDispatcher.setNextChainDispatcher(nextChainHandler.commandDispatcher);
    }

    public ProcessUserCommandResponse handleRequest(ProcessUserCommandRequest request) {
        return commandDispatcher.handleRequest(request);
    }

    public ProcessUserCommandResponse onCommandHelp(CommandHelp commandHelp) {
        return helpHandler.onCommandHelp(commandHelp);
    }

}
