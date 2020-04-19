package io.mamish.serverbot2.commandlambda;

import io.mamish.serverbot2.commandlambda.model.commands.CommandHandler;
import io.mamish.serverbot2.commandlambda.model.service.UserCommandRequest;
import io.mamish.serverbot2.commandlambda.model.service.UserCommandResponse;
import io.mamish.serverbot2.discordrelay.model.service.DiscordRequestHandler;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedutil.Pair;
import io.mamish.serverbot2.sharedutil.reflect.AbstractRequestDispatcher;
import io.mamish.serverbot2.sharedutil.reflect.UnparsableInputException;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CommandDispatcher extends AbstractRequestDispatcher<CommandHandler, List<String>, List<String>, CommandDefinition> {

    private static final String SIGIL = CommonConfig.COMMAND_SIGIL_CHARACTER;

    private CommandHandler commandHandlerInstance;
    private Map<String, CommandDefinition> commandDefinitions;

    public CommandDispatcher(CommandHandler handler) {
        super(handler, CommandHandler.class, CommandDefinition.class);
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
                return (UserCommandResponse) definition.getTargetMethod().invoke(commandHandlerInstance, requestObject);
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
    protected Pair<String, List<String>> parseNameKey(List<String> input) throws UnparsableInputException {
        if (input.size() < 1) {
            throw new UnparsableInputException("Empty word list: expected at least the command/keyword.");
        }
        String name = input.get(0);
        List<String> arguments = input.subList(1,input.size());
        return new Pair<>(name,arguments);
    }

    @Override
    protected Object parseRequestObject(CommandDefinition definition, List<String> input) {

    }
}
