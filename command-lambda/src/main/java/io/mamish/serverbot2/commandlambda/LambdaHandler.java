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

    private Map<String, GeneratedCommandDefinition> commandDefinitions;

    public LambdaHandler() {

        var allListenerInterfaceMethods = Arrays.stream(Listener.class.getDeclaredMethods());
        Function<Method,GeneratedCommandDefinition> tryGenerateDefinition = method -> {
            try {
                return new GeneratedCommandDefinition(method);
            } catch (ReflectiveOperationException roe) {
                throw new IllegalStateException("Invalid command definitions", roe);
            }
        };
        var compareByDocPositionAttribute = Comparator.comparing(GeneratedCommandDefinition::getDocumentationPosition);
        BinaryOperator<GeneratedCommandDefinition> errorOnNameCollision = (_a, _b) -> {
            throw new RuntimeException("Command name collision while generating definitions: " + _a.getName());
        };
        var collectToTreeMapWithNameAsKey = Collectors.toMap(GeneratedCommandDefinition::getName,
                Function.identity(),
                errorOnNameCollision,
                TreeMap::new);

        this.commandDefinitions = allListenerInterfaceMethods
                .map(tryGenerateDefinition)
                .sorted(compareByDocPositionAttribute)
                .collect(collectToTreeMapWithNameAsKey);
    }

    @Override
    public UserCommandResponse handleRequest(UserCommandRequest userCommandRequest, Context context) {

        List<String> commandWords = userCommandRequest.getWords();
        String keyword = commandWords.get(0);
        List<String> args = commandWords.subList(1, commandWords.size());

        GeneratedCommandDefinition definition = commandDefinitions.get(keyword);

        // Check for unknown command
        if (definition == null) {
            String errorMessage = String.format("Error: !%s is not a recognised command.", keyword);
            return new UserCommandResponse(errorMessage, null);
        }

        // Check for not all required arguments provided
        if (args.size() < definition.getNumRequiredFields()) {
            String pluralS = (definition.getNumRequiredFields() > 1) ? "s" : "";
            String fstr = "Error: expected at least %d argument%s but got %d."
                    + "\nUsage: %s"
                    + "\nUse '!help %s' for details.";
            String errorMessage = String.format(fstr,
                    definition.getNumRequiredFields(), pluralS, args.size(), definition.getUsageString(), keyword);
            return new UserCommandResponse(errorMessage, null);
        }

        // Try parsing the request object (shouldn't fail based on above checks).
        try {
            Object requestObject = definition.getRequestTypeConstructor().newInstance();
            for (int i = 0; i < args.size() && i < definition.getOrderedFields().size(); i++) {
                Field field = definition.getOrderedFields().get(i);
                String value = args.get(i);
                field.set(requestObject, value);
            }

            // If parsing worked, run target listener method with request object.
            try {
                return (UserCommandResponse) definition.getTargetMethod().invoke(this, requestObject);
            } catch (InvocationTargetException ite) {
                ite.printStackTrace();
                return new UserCommandResponse("Unexpected error occurred while running command. Sorry...", null);
            }

        } catch (ReflectiveOperationException roe) {
            roe.printStackTrace();
            return new UserCommandResponse("Unexpected error occurred while parsing command. Sorry...", null);
        }


    }

    @Override
    public UserCommandResponse onCommandHelp(CommandHelp commandHelp) {

        if (commandHelp.getCommandName() != null) {
            String name = commandHelp.getCommandName();
            GeneratedCommandDefinition definition = commandDefinitions.get(name);
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
            String aggregateHelpString = commandDefinitions.values().stream().map(definition -> {
                String line0 = definition.getUsageString();
                String line1 = definition.getDescriptionString();
                return line0 + "\n  " + line1;
            }).collect(Collectors.joining("\n"));
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
