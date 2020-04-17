package io.mamish.serverbot2.commandlambda;

import io.mamish.serverbot2.commandlambda.model.commands.CommandListener;
import io.mamish.serverbot2.commandlambda.model.service.UserCommandRequest;
import io.mamish.serverbot2.commandlambda.model.service.UserCommandResponse;
import io.mamish.serverbot2.sharedconfig.CommonConfig;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CommandDispatcher {

    private static final String SIGIL = CommonConfig.COMMAND_SIGIL_CHARACTER;

    private CommandListener commandListenerInstance;
    private Map<String, CommandDefinition> commandDefinitions;

    public CommandDispatcher(CommandListener commandListenerInstance) {

        this.commandListenerInstance = commandListenerInstance;

        generateCommandDefinitions();
    }

    private void generateCommandDefinitions() {

        var allListenerInterfaceMethods = Arrays.stream(CommandListener.class.getDeclaredMethods());
        Function<Method, CommandDefinition> tryGenerateDefinition = method -> {
            try {
                return new CommandDefinition(method);
            } catch (ReflectiveOperationException roe) {
                throw new IllegalStateException("Invalid command definitions", roe);
            }
        };
        var compareByDocPositionAttribute = Comparator.comparing(CommandDefinition::getDocumentationPosition);
        BinaryOperator<CommandDefinition> errorOnNameCollision = (_a, _b) -> {
            throw new RuntimeException("Command name collision while generating definitions: " + _a.getName());
        };
        var collectToTreeMapWithNameAsKey = Collectors.toMap(CommandDefinition::getName,
                Function.identity(),
                errorOnNameCollision,
                TreeMap::new);

        this.commandDefinitions = allListenerInterfaceMethods
                .map(tryGenerateDefinition)
                .sorted(compareByDocPositionAttribute)
                .collect(collectToTreeMapWithNameAsKey);

    }

    public Map<String,CommandDefinition> getDefinitions() {
        return Collections.unmodifiableMap(commandDefinitions);
    }

    public UserCommandResponse dispatch(UserCommandRequest userCommandRequest) {
        List<String> commandWords = userCommandRequest.getWords();
        String keyword = commandWords.get(0);
        List<String> args = commandWords.subList(1, commandWords.size());

        CommandDefinition definition = commandDefinitions.get(keyword);

        // Check for unknown command
        if (definition == null) {
            String errorMessage = String.format("Error: %s%s is not a recognised command.", SIGIL, keyword);
            return new UserCommandResponse(errorMessage, null);
        }

        // Check for not all required arguments provided
        if (args.size() < definition.getNumRequiredFields()) {
            String pluralS = (definition.getNumRequiredFields() > 1) ? "s" : "";
            String fstr = "Error: expected at least %d argument%s but got %d."
                    + "\nUsage: %s"
                    + "\nUse '"+SIGIL+"help %s' for details.";
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
                return (UserCommandResponse) definition.getTargetMethod().invoke(commandListenerInstance, requestObject);
            } catch (InvocationTargetException ite) {
                ite.printStackTrace();
                return new UserCommandResponse("Unexpected error occurred while running command. Sorry...", null);
            }

        } catch (ReflectiveOperationException roe) {
            roe.printStackTrace();
            return new UserCommandResponse("Unexpected error occurred while parsing command. Sorry...", null);
        }
    }

}
