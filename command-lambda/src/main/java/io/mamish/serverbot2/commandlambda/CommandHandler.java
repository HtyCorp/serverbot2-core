package io.mamish.serverbot2.commandlambda;

import io.mamish.serverbot2.commandlambda.model.commands.*;
import io.mamish.serverbot2.commandlambda.model.service.CommandServiceRequest;
import io.mamish.serverbot2.commandlambda.model.service.CommandServiceResponse;

import java.lang.reflect.InvocationTargetException;
import java.util.stream.Collectors;

public class CommandHandler implements ICommandHandler {

    private CommandDispatcher commandDispatcher;

    public CommandHandler() {
        commandDispatcher = new CommandDispatcher(this);
    }

    public CommandServiceResponse handleRequest(CommandServiceRequest request) {
        try {
            return commandDispatcher.dispatch(request);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Uknown exception when invoking command.", e);
        }
    }

    @Override
    public CommandServiceResponse onCommandHelp(CommandHelp commandHelp) {

        if (commandHelp.getCommandName() != null) {
            String name = commandHelp.getCommandName();
            CommandDefinition definition = commandDispatcher.getDefinitionMap().get(name);
            if (definition == null) {
                return new CommandServiceResponse("Error: '" + name + "' is not a recognised command name.");
            } else {
                StringBuilder detailedHelpBuilder = new StringBuilder();
                detailedHelpBuilder.append(definition.getUsageString());
                detailedHelpBuilder.append("\n  ").append(definition.getDescription());
                for (String argString: definition.getArgumentDescriptionStrings()) {
                    detailedHelpBuilder.append("\n    ").append(argString);
                }
                return new CommandServiceResponse(detailedHelpBuilder.toString());
            }
        } else {
            String aggregateHelpString = commandDispatcher.getDefinitionMap().values().stream()
                    .map(definition -> definition.getUsageString() + "\n  " + definition.getDescription())
                    .collect(Collectors.joining("\n"));
            return new CommandServiceResponse(aggregateHelpString);
        }
    }

    @Override
    public CommandServiceResponse onCommandGames(CommandGames commandGames) {
        return null;
    }

    @Override
    public CommandServiceResponse onCommandStart(CommandStart commandStart) {
        return null;
    }

    @Override
    public CommandServiceResponse onCommandStop(CommandStop commandDtoStop) {
        return null;
    }

    @Override
    public CommandServiceResponse onCommandAddIp(CommandDtoAddIp commandAddIp) {
        return null;
    }

}
