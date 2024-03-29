package com.admiralbot.framework.server;

import com.admiralbot.sharedconfig.LambdaWarmerConfig;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

// Uses stream handler: Lambda runtime with RequestHandler<String,T> expects a double-quote enclosed JSON string,
// but clients in this framework send an unquoted JSON object.
public abstract class LambdaApiServer<ModelType> extends AbstractApiServer<ModelType> implements RequestStreamHandler {

    private final Logger logger = LoggerFactory.getLogger(LambdaApiServer.class);

    public LambdaApiServer() {
        super.initialise();
    }

    @Override
    protected boolean requiresEndpointInfo() {
        return false;
    }

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        // Note: This does not use context.getLogger() for logging since I haven't mocked it in ApiClient.localLambda
        // (which passes a null context at the moment).

        String inputString = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

        if (inputString.equals(LambdaWarmerConfig.WARMER_PING_LAMBDA_PAYLOAD)) {
            logger.info("Warmer ping request");
            return;
        }

        logger.info("Request payload:\n" + inputString);
        String outputString = getRequestDispatcher().handleRequest(inputString);
        logger.info("Response payload:\n" + outputString);

        outputStream.write(outputString.getBytes(StandardCharsets.UTF_8));
    }
}