package io.mamish.serverbot2.commandlambda;

import io.mamish.serverbot2.commandlambda.model.commands.*;
import io.mamish.serverbot2.commandlambda.model.service.CommandServiceRequest;
import io.mamish.serverbot2.commandlambda.model.service.CommandServiceResponse;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedutil.reflect.RequestHandlingException;
import io.mamish.serverbot2.sharedutil.reflect.RequestValidationException;
import io.mamish.serverbot2.sharedutil.reflect.UnknownRequestException;

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
        } catch(UnknownRequestException e) {
            return new CommandServiceResponse("Error: '"+e.getRequestedTarget()+"' is not a recognised command.");
        } catch(RequestValidationException | RequestHandlingException e) {
            return new CommandServiceResponse("Error: " + e.getMessage());
        // Other exception types should be treated as non-publishable for UX, so don't return a response object.
        } catch(InvocationTargetException e) {
            throw new RuntimeException("Unknown exception when invoking command.", e);
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
