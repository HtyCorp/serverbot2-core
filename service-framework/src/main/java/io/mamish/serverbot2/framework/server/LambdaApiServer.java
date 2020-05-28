package io.mamish.serverbot2.framework.server;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public abstract class LambdaApiServer<ModelType> implements RequestStreamHandler {

    private JsonApiRequestDispatcher<ModelType> jsonApiHandler = new JsonApiRequestDispatcher<>(getHandlerInstance(),getModelClass());

    protected abstract Class<ModelType> getModelClass();
    protected abstract ModelType getHandlerInstance();

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        String inputString = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        System.out.println("Request payload:");
        System.out.println(inputString);

        String outputString = jsonApiHandler.handleRequest(inputString);
        System.out.println("Response payload:");
        System.out.println(outputString);

        outputStream.write(outputString.getBytes(StandardCharsets.UTF_8));
    }
}
