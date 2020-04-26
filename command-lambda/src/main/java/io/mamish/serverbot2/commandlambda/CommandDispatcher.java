package io.mamish.serverbot2.commandlambda;

import io.mamish.serverbot2.commandlambda.model.commands.AbstractCommandDto;
import io.mamish.serverbot2.commandlambda.model.commands.ICommandHandler;
import io.mamish.serverbot2.commandlambda.model.service.CommandServiceRequest;
import io.mamish.serverbot2.commandlambda.model.service.CommandServiceResponse;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedutil.Pair;
import io.mamish.serverbot2.sharedutil.reflect.AbstractRequestDispatcher;
import io.mamish.serverbot2.sharedutil.reflect.RequestValidationException;
import io.mamish.serverbot2.sharedutil.reflect.SerializationException;
import io.mamish.serverbot2.sharedutil.reflect.UnparsableInputException;

import java.lang.reflect.Field;
import java.util.List;

public class CommandDispatcher extends AbstractRequestDispatcher<ICommandHandler,CommandServiceRequest,CommandServiceRequest,CommandServiceResponse,CommandDefinition> {

    private static final String SIGIL = CommonConfig.COMMAND_SIGIL_CHARACTER;

    public CommandDispatcher(ICommandHandler handler) {
        super(handler, ICommandHandler.class, CommandDefinition::new);
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
    protected Object parseRequestObject(CommandDefinition definition, CommandServiceRequest inputRequest) {

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
            Object baseRequestObject = definition.getRequestDtoConstructor().newInstance();
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
    protected CommandServiceResponse serializeResponseObject(CommandDefinition definition, Object handlerResult) {
        if (!(handlerResult instanceof CommandServiceResponse)) {
            throw new SerializationException("Command handler returned a type other than UserCommandResponse");
        }
        return (CommandServiceResponse) handlerResult;
    }

}
