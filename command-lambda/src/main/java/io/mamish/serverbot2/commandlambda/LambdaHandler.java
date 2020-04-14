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

public class LambdaHandler implements RequestHandler<UserCommandRequest, UserCommandResponse>, Listener {

    private Map<String, GeneratedCommandDefinition> commandDefinitions = new TreeMap<>();

    public LambdaHandler() {
        for (Method method: Listener.class.getDeclaredMethods()) {
            try {
                GeneratedCommandDefinition definition = new GeneratedCommandDefinition(method);
                commandDefinitions.put(definition.getName(), definition);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Invalid command definitions", e);
            }
        }
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
        return null;
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
