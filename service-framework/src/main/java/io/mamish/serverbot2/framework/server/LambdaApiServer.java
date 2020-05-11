package io.mamish.serverbot2.framework.server;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public abstract class LambdaApiServer<ModelType> implements RequestHandler<String,String> {

    private JsonApiRequestDispatcher<ModelType> jsonApiHandler = new JsonApiRequestDispatcher<>(getHandlerInstance(),getModelClass());

    protected abstract Class<ModelType> getModelClass();
    protected abstract ModelType getHandlerInstance();

    @Override
    public final String handleRequest(String input, Context context) {
        return jsonApiHandler.handleRequest(input);
    }

}
