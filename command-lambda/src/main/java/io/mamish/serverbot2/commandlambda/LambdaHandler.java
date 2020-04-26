package io.mamish.serverbot2.commandlambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import io.mamish.serverbot2.commandlambda.model.service.CommandServiceRequest;
import io.mamish.serverbot2.commandlambda.model.service.CommandServiceResponse;
import io.mamish.serverbot2.commandlambda.model.service.ICommandServiceHandler;
import io.mamish.serverbot2.sharedutil.AnnotatedGson;
import io.mamish.serverbot2.sharedutil.reflect.JsonRequestDispatcher;
import io.mamish.serverbot2.sharedutil.reflect.RequestHandlingRuntimeException;
/*
 * This is just an entry layer to parse JSON and build a service command to pass the real command handler.
 * Lambda native JSON parsing makes this extra layer a bit unnecessary, but I'm keeping it for testing and consistency.
 */
public class LambdaHandler implements RequestHandler<String, String>, ICommandServiceHandler {

    private JsonRequestDispatcher<ICommandServiceHandler> requestDispatcher;
    private CommandHandler commandHandler;
    private AnnotatedGson gson = new AnnotatedGson();

    public LambdaHandler() {
        requestDispatcher = new JsonRequestDispatcher<>(this, ICommandServiceHandler.class);
        commandHandler = new CommandHandler();
    }

    @Override
    public String handleRequest(String inputString, Context context) {
        try {
            return requestDispatcher.dispatch(inputString);
        } catch (RequestHandlingRuntimeException e) {
            e.printStackTrace();
            CommandServiceResponse defaultErrorResponse = new CommandServiceResponse("Sorry, an unknown error occurred.");
            return gson.toJson(defaultErrorResponse);
        }
    }

    @Override
    public CommandServiceResponse onRequestUserCommand(CommandServiceRequest commandServiceRequest) {
        return commandHandler.handleRequest(commandServiceRequest);
    }

}
