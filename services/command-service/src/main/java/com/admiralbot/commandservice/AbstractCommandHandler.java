package com.admiralbot.commandservice;

import com.admiralbot.commandservice.commands.common.CommandHelp;
import com.admiralbot.commandservice.model.ProcessUserCommandRequest;
import com.admiralbot.commandservice.model.ProcessUserCommandResponse;
import com.admiralbot.framework.common.ApiDefinitionSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractCommandHandler<ModelType> {

    private final Logger logger = LoggerFactory.getLogger(AbstractCommandHandler.class);

    private final CommandDispatcher<ModelType> commandDispatcher;
    private final HelpMessageHelper helpMessageHelper;
    // AbstractApiRequestDispatcher already has a generic chaining mechanism for when a command isn't found.
    // For better UX, we need some extra chaining specifically for '/help <command-name>' requests.
    private AbstractCommandHandler<?> nextCommandHandler;

    protected abstract Class<ModelType> getHandlerType();
    protected abstract ModelType getHandlerInstance();

    public AbstractCommandHandler() {
        logger.trace("Building command dispatcher");
        commandDispatcher = new CommandDispatcher<>(getHandlerInstance(), getHandlerType());
        helpMessageHelper = new HelpMessageHelper(commandDispatcher.getApiDefinitionSet());
        logger.trace("Constructor finished");
    }

    public void setNextChainHandler(AbstractCommandHandler<?> nextChainHandler) {
        this.nextCommandHandler = nextChainHandler;
        commandDispatcher.setNextChainDispatcher(nextChainHandler.commandDispatcher);
    }

    public ProcessUserCommandResponse handleRequest(ProcessUserCommandRequest request) {
        return commandDispatcher.handleRequest(request);
    }

    public ProcessUserCommandResponse onCommandHelp(CommandHelp commandHelp) {
        if (commandHelp.getCommandName() != null &&
                nextCommandHandler != null &&
                !helpMessageHelper.hasDefinitionFor(commandHelp.getCommandName())){
            logger.info("Got a request to look up help for a non-existent command - passing to next handler");
            return nextCommandHandler.onCommandHelp(commandHelp);
        }
        return helpMessageHelper.onCommandHelp(commandHelp);
    }

    public ApiDefinitionSet<ModelType> getCommandDefinitionSet() {
        return commandDispatcher.getApiDefinitionSet();
    }
}
