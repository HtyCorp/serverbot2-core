package io.mamish.serverbot2.commandlambda;

import io.mamish.serverbot2.commandlambda.model.commands.AbstractCommandDto;
import io.mamish.serverbot2.commandlambda.model.commands.ICommandHandler;
import io.mamish.serverbot2.commandlambda.model.service.CommandServiceRequest;
import io.mamish.serverbot2.commandlambda.model.service.CommandServiceResponse;
import io.mamish.serverbot2.framework.common.ApiActionDefinition;
import io.mamish.serverbot2.framework.exception.ApiException;
import io.mamish.serverbot2.framework.exception.server.*;
import io.mamish.serverbot2.framework.server.AbstractApiRequestDispatcher;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedutil.ClassUtils;
import io.mamish.serverbot2.sharedutil.Pair;

import java.lang.reflect.Field;
import java.util.List;

public class CommandDispatcher extends AbstractApiRequestDispatcher<ICommandHandler,CommandServiceResponse,CommandServiceRequest,CommandServiceRequest> {

    private static final String SIGIL = CommonConfig.COMMAND_SIGIL_CHARACTER;

    // These exception types report the exact exception message to the user if caught.
    // Others are unexpected or confusing, and return a generic error message.
    private static final Class<?>[] allowedExceptionsForUserMessage = new Class<?>[] {
            RequestHandlingException.class, // Explicitly thrown handling exceptions have crafted messages.
            RequestValidationException.class, // Used for invalid or missing request arguments.
            UnknownRequestException.class, // Used if an unknown command is requested by user.
    };

    public CommandDispatcher(ICommandHandler handler) {
        super(handler, ICommandHandler.class);
    }

    @Override
    protected Pair<String, CommandServiceRequest> parseNameKey(CommandServiceRequest input) {
        List<String> words = input.getWords();
        if (words.size() < 1) {
            throw new UnparsableInputException("Empty word list: expected at least the command/keyword.");
        }
        String name = words.get(0);
        return new Pair<>(name,input);
    }

    @Override
    protected Object parseRequestObject(ApiActionDefinition definition, CommandServiceRequest inputRequest) {

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
            requestDto.setOriginalRequest(inputRequest);

            for (int i = 0; i < arguments.size() && i < definition.getOrderedFields().size(); i++) {
                Field field = definition.getOrderedFieldsFieldView().get(i);
                String value = arguments.get(i);
                field.set(requestDto, value);
            }

            return requestDto;

        } catch (ReflectiveOperationException e) {
            throw new UnparsableInputException("Unknown error while processing command.", e);
        }
    }

    @Override
    protected CommandServiceResponse serializeResponseObject(ApiActionDefinition definition, Object handlerResult) {
        if (!(handlerResult instanceof CommandServiceResponse)) {
            throw new SerializationException("Command handler returned a type other than CommandServiceResponse");
        }
        return (CommandServiceResponse) handlerResult;
    }

    @Override
    protected CommandServiceResponse serializeErrorObject(ApiException exception) {
        if (ClassUtils.instanceOfAny(exception, allowedExceptionsForUserMessage)) {
            return new CommandServiceResponse("Error: " + exception.getMessage());
        } else {
            return new CommandServiceResponse("Sorry, an unknown error occurred.");
        }
    }

}
