package com.admiralbot.commandservice;

import com.admiralbot.commandservice.commands.AbstractCommandDto;
import com.admiralbot.commandservice.model.ProcessUserCommandRequest;
import com.admiralbot.commandservice.model.ProcessUserCommandResponse;
import com.admiralbot.framework.common.ApiActionDefinition;
import com.admiralbot.framework.exception.server.*;
import com.admiralbot.framework.server.AbstractApiRequestDispatcher;
import com.admiralbot.sharedconfig.CommonConfig;
import com.admiralbot.sharedutil.ClassUtils;
import com.admiralbot.sharedutil.Pair;

import java.lang.reflect.Field;
import java.util.List;

public class CommandDispatcher<ModelType> extends AbstractApiRequestDispatcher<ModelType, ProcessUserCommandResponse,
        ProcessUserCommandRequest, ProcessUserCommandRequest> {

    private static final String SIGIL = CommonConfig.COMMAND_SIGIL_CHARACTER;

    /*
     * Some ApiServerException subtypes are thrown by the dispatcher superclass, so their messages aren't user-
     * friendly. If caught, they return a generic error message in Discord. The types in this array, meanwhile, are
     * created in this module specifically, so their messages are user-friendly and can be passed on directly.
     */
    private static final Class<?>[] allowedExceptionsForUserMessage = new Class<?>[] {
            RequestHandlingException.class, // Explicitly thrown handling exceptions have crafted messages.
            RequestValidationException.class, // Used for invalid or missing request arguments.
    };

    public CommandDispatcher(ModelType handler, Class<ModelType> modelTypeClass) {
        super(handler, modelTypeClass, false);
    }

    @Override
    protected Pair<String, ProcessUserCommandRequest> parseNameKey(ProcessUserCommandRequest input) {
        List<String> words = input.getWords();
        if (words.size() < 1) {
            throw new UnparsableInputException("Empty word list: expected at least the command/keyword.");
        }
        String name = words.get(0);
        return new Pair<>(name,input);
    }

    @Override
    protected Object parseRequestObject(ApiActionDefinition definition, ProcessUserCommandRequest inputRequest) {

        String name = definition.getName();
        List<String> arguments = inputRequest.getWords().subList(1,inputRequest.getWords().size());

        // Check for not all required arguments provided
        if (arguments.size() < definition.getNumRequiredFields()) {
            String pluralS = (definition.getNumRequiredFields() > 1) ? "s" : "";
            String fstr = "Expected at least %d argument%s but got %d."
                    + "\nUsage: %s"
                    + "\nUse '"+SIGIL+"help %s' for details.";
            String errorMessage = String.format(fstr,
                    definition.getNumRequiredFields(), pluralS, arguments.size(), definition.getUsageString(), name);
            throw new RequestValidationException(errorMessage);
        }

        // Try parsing the request object (shouldn't fail based on above checks).
        try {
            Object baseRequestObject = definition.getRequestTypeConstructor().newInstance();
            if (!(baseRequestObject instanceof AbstractCommandDto)) {
                throw new IllegalStateException("Illegal DTO type: not an subclass of AbstractCommandDto");
            }
            AbstractCommandDto requestDto = (AbstractCommandDto) baseRequestObject;
            requestDto.setContext(inputRequest);

            for (int i = 0; i < arguments.size() && i < definition.getOrderedFields().size(); i++) {
                Field field = definition.getOrderedFieldsFieldView().get(i);
                trySetField(requestDto, field, arguments.get(i));
            }

            return requestDto;

        } catch (ReflectiveOperationException e) {
            throw new UnparsableInputException("Unknown error while processing command.", e);
        }
    }

    private void trySetField(AbstractCommandDto object, Field field, String argumentString) throws IllegalAccessException{
        Class<?> fieldType = field.getType();
        if (fieldType.equals(String.class)) {
            field.set(object, argumentString);
        } else if (fieldType.equals(Integer.TYPE)) {
            try {
                int parsedArgument = Integer.parseInt(argumentString);
                field.set(object, parsedArgument);
            } catch (NumberFormatException e) {
                throw new RequestValidationException(String.format(
                    "Validation error: input '%s' for argument %s isn't a valid whole number.",
                    argumentString, field.getName()
                ));
            }
        } else {
            throw new RequestHandlingException(String.format(
                    "Internal error: field %s isn't a valid parseable type",
                    field.getName()
            ));
        }
    }

    @Override
    protected ProcessUserCommandResponse serializeResponseObject(ApiActionDefinition definition, Object handlerResult) {
        // All ICommandService methods return CommandServiceResponse
        if (!(handlerResult instanceof ProcessUserCommandResponse)) {
            throw new SerializationException("Command handler returned a type other than CommandServiceResponse");
        }
        return (ProcessUserCommandResponse) handlerResult;
    }

    // This method is responsible for generating user messages in Discord from a server error.
    @Override
    protected ProcessUserCommandResponse serializeErrorObject(ApiServerException exception) {
        // UnknownRequestException message is generated by the superclass, so it needs a custom user-friendly message.
        if (exception instanceof UnknownRequestException) {
            String name = ((UnknownRequestException)exception).getRequestedTargetName();
            return new ProcessUserCommandResponse("Error: '" + name + "' is not a recognised command.");
        } else if (ClassUtils.instanceOfAny(exception, allowedExceptionsForUserMessage)) {
            return new ProcessUserCommandResponse("Error: " + exception.getMessage());
        } else {
            return new ProcessUserCommandResponse("Sorry, an unknown error occurred.");
        }
    }

}
