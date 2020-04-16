package io.mamish.serverbot2.commandlambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import io.mamish.serverbot2.commandlambda.model.commands.*;
import io.mamish.serverbot2.commandlambda.model.service.UserCommandRequest;
import io.mamish.serverbot2.commandlambda.model.service.UserCommandResponse;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LambdaHandler implements RequestHandler<UserCommandRequest, UserCommandResponse>, Listener {

    private CommandDispatcher commandDispatcher;

    public LambdaHandler() {
        commandDispatcher = new CommandDispatcher(this);
    }

    @Override
    public UserCommandResponse handleRequest(UserCommandRequest userCommandRequest, Context context) {
        return commandDispatcher.dispatch(userCommandRequest);
    }

    @Override
    public UserCommandResponse onCommandHelp(CommandHelp commandHelp) {

        if (commandHelp.getCommandName() != null) {
            String name = commandHelp.getCommandName();
            CommandDefinition definition = commandDispatcher.getDefinitions().get(name);
            if (definition == null) {
                return new UserCommandResponse("Error: '" + name + "' is not a recognised command name.");
            } else {
                StringBuilder detailedHelpBuilder = new StringBuilder();
                detailedHelpBuilder.append(definition.getUsageString());
                detailedHelpBuilder.append("\n  ").append(definition.getDescriptionString());
                for (String argString: definition.getArgumentDescriptionStrings()) {
                    detailedHelpBuilder.append("\n    ").append(argString);
                }
                return new UserCommandResponse(detailedHelpBuilder.toString());
            }
        } else {
            String aggregateHelpString = commandDispatcher.getDefinitions().values().stream()
                    .map(definition -> definition.getUsageString() + "\n  " + definition.getDescriptionString())
                    .collect(Collectors.joining("\n"));
            return new UserCommandResponse(aggregateHelpString);
        }
    }

    @Override
    public UserCommandResponse onCommandGames(CommandGames commandGames) {
        return null;
    }

    @Override
    public UserCommandResponse onCommandStart(CommandStart commandStart) {
        return null;
    }

    @Override
    public UserCommandResponse onCommandStop(CommandStop commandStop) {
        return null;
    }

    @Override
    public UserCommandResponse onCommandAddIp(CommandAddIp commandAddIp) {
        return null;
    }

}
