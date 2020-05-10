package io.mamish.serverbot2.framework.server;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public abstract class LambdaStandardApiHandler<ModelType> implements RequestHandler<String,String> {

    private JsonApiHandler<ModelType> jsonApiHandler = new JsonApiHandler<>(getHandlerInstance(),getModelClass());

    protected abstract Class<ModelType> getModelClass();
    protected abstract ModelType getHandlerInstance();

    @Override
    public String handleRequest(String input, Context context) {
        return jsonApiHandler.handleRequest(input);
    }

}
