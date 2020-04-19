package io.mamish.serverbot2.commandlambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import io.mamish.serverbot2.commandlambda.model.service.CommandServiceRequest;
import io.mamish.serverbot2.commandlambda.model.service.ICommandServiceHandler;
import io.mamish.serverbot2.commandlambda.model.service.CommandServiceResponse;
import io.mamish.serverbot2.sharedutil.reflect.SimpleApiDefinition;
import io.mamish.serverbot2.sharedutil.reflect.JsonRequestDispatcher;

import java.lang.reflect.InvocationTargetException;
/*
 * This is just an entry layer to parse JSON and build a service command to pass the real command handler.
 * Lambda native JSON parsing makes this extra layer a bit unnecessary, but I'm keeping it for testing and consistency.
 */
public class LambdaHandler implements RequestHandler<String, String>, ICommandServiceHandler {

    private JsonRequestDispatcher<ICommandServiceHandler> requestDispatcher;
    private CommandHandler commandHandler;

    public LambdaHandler() {
        requestDispatcher = new JsonRequestDispatcher<>(this, ICommandServiceHandler.class, SimpleApiDefinition.class);
        commandHandler = new CommandHandler();
    }

    @Override
    public String handleRequest(String inputString, Context context) {
        try {
            return requestDispatcher.dispatch(inputString);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Uncaught exception when handling request", e);
        }
    }

    @Override
    public CommandServiceResponse onRequestUserCommand(CommandServiceRequest commandServiceRequest) {
        return commandHandler.handleRequest(commandServiceRequest);
    }
}
