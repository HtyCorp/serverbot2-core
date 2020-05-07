package io.mamish.serverbot2.gamemetadata;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import io.mamish.serverbot2.gamemetadata.model.IGameMetadataServiceHandler;
import io.mamish.serverbot2.sharedutil.reflect.JsonRequestDispatcher;

public class LambdaHandler implements RequestHandler<String,String> {

    private GameMetadataServiceHandler serviceHandler;
    private JsonRequestDispatcher<IGameMetadataServiceHandler> dispatcher;

    public LambdaHandler() {
        serviceHandler = new GameMetadataServiceHandler();
        dispatcher = new JsonRequestDispatcher<>(serviceHandler, IGameMetadataServiceHandler.class);
    }

    @Override
    public String handleRequest(String input, Context context) {
        return dispatcher.dispatch(input);
    }
}
