package io.mamish.serverbot2.commandlambda;

import com.google.gson.Gson;
import io.mamish.serverbot2.commandlambda.model.commands.*;
import io.mamish.serverbot2.commandlambda.model.service.CommandServiceRequest;
import io.mamish.serverbot2.commandlambda.model.service.CommandServiceResponse;
import io.mamish.serverbot2.sharedutil.reflect.RequestHandlingException;
import io.mamish.serverbot2.sharedutil.reflect.RequestValidationException;
import io.mamish.serverbot2.sharedutil.reflect.UnknownRequestException;

import java.util.stream.Collectors;

public class CommandHandler implements ICommandHandler {

    private CommandDispatcher commandDispatcher;
    private Gson gson;

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
        }
        // Other exception types should be treated as non-publishable for UX, so don't return a response object.
    }

    @Override
    public CommandServiceResponse onCommandHelp(CommandHelp commandHelp) {

        if (commandHelp.getCommandName() != null) {
            String name = commandHelp.getCommandName();
            CommandDefinition definition = commandDispatcher.getDefinitionMap().get(name);
            if (definition == null) {
                return new CommandServiceResponse("Can't look up help: '" + name + "' is not a recognised command name.");
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
        return new CommandServiceResponse("Echo 'games': " + gson.toJson(commandGames));
    }

    @Override
    public CommandServiceResponse onCommandStart(CommandStart commandStart) {
        return new CommandServiceResponse("Echo 'start': " + gson.toJson(commandStart));
    }

    @Override
    public CommandServiceResponse onCommandStop(CommandStop commandStop) {
        return new CommandServiceResponse("Echo 'stop': " + gson.toJson(commandStop));
    }

    @Override
    public CommandServiceResponse onCommandAddIp(CommandAddIp commandAddIp) {
        return new CommandServiceResponse("Echo 'addip': " + gson.toJson(commandAddIp));
    }

}
