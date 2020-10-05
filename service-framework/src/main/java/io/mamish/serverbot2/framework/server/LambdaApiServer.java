package io.mamish.serverbot2.framework.server;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import io.mamish.serverbot2.sharedconfig.LambdaWarmerConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

// Uses stream handler: Lambda runtime with RequestHandler<String,T> expects a double-quote enclosed JSON string,
// but clients in this framework send an unquoted JSON object.
public abstract class LambdaApiServer<ModelType> implements RequestStreamHandler {

    private final Logger logger = LogManager.getLogger(LambdaApiServer.class);

    private final JsonApiRequestDispatcher<ModelType> jsonApiHandler = new JsonApiRequestDispatcher<>(createHandlerInstance(),getModelClass());

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
     * Create a new instance of <code>ModelType</code> to handle API requests for the given service model.
     * <p>
     * Warning: this is called during the super constructor in LambdaApiServer, which runs <b>before</b> any instance
     * field initialization in the subclass. You cannot refer to any instance fields since they will be null at this
     * point.
     * <p>
     * This class will attempt to parse the payload of Lambda invocations as requests in the given service, and dispatch
     * them to the provided handler.
     *
     * @return An instance of <code>ModelType</code> to handle API requests
     */
    protected abstract ModelType createHandlerInstance();

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        // Note: This does not use context.getLogger() for logging since I haven't mocked it in ApiClient.localLambda
        // (which passes a null context at the moment).

        String inputString = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

        if (inputString.equals(LambdaWarmerConfig.LAMBDA_WARMER_PING_STRING)) {
            logger.info("Warmer ping request");
            return;
        }

        logger.info("Request payload:\n" + inputString);
        String outputString = jsonApiHandler.handleRequest(inputString);
        logger.info("Response payload:\n" + outputString);

        outputStream.write(outputString.getBytes(StandardCharsets.UTF_8));
    }
}
