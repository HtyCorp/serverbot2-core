package io.mamish.serverbot2.framework.server;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

// Uses stream handler: Lambda runtime with RequestHandler<String,T> expects a double-quote enclosed JSON string,
// but clients in this framework send an unquoted JSON object.
public abstract class LambdaApiServer<ModelType> implements RequestStreamHandler {

    private final JsonApiRequestDispatcher<ModelType> jsonApiHandler = new JsonApiRequestDispatcher<>(getHandlerInstance(),getModelClass());

    /**
     * <p>
     * Must return the class of generic parameter <code>ModelType</code>.
     * <p>
     * Due to type erasure, the class corresponding to a given generic parameter can't be retrieved dynamically at
     * runtime, so it needs to be explicitly provided by the subclass.
     *
     * @return The class of generic parameter <code>ModelType</code>
     */
    protected abstract Class<ModelType> getModelClass();

    /**
     * <p>
     * Must return an instance of <code>ModelType</code> to handle API requests for the given service model. This is
     * typically the same class, for example:
     * <p>
     * <code>public class MyHandler extends LambdaApiServer{@literal <MyModel>}
     * implements MyModel { ... protected MyModel getHandlerInstance() { return this; }</code>.
     * <p>
     * This class will attempt to parse the payload of Lambda invocations as requests in the given service, and dispatch
     * them to the provided handler.
     *
     * @return An instance of <code>ModelType</code> to handle API requests
     */
    protected abstract ModelType getHandlerInstance();

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        // Note: This does not use context.getLogger() for logging since I haven't mocked it in ApiClient.localLambda
        // (which passes a null context at the moment).

        String inputString = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        System.out.println("Request payload:");
        System.out.println(inputString);

        String outputString = jsonApiHandler.handleRequest(inputString);
        System.out.println("Response payload:");
        System.out.println(outputString);

        outputStream.write(outputString.getBytes(StandardCharsets.UTF_8));
    }
}
