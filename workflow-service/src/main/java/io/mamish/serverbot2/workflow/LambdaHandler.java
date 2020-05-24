package io.mamish.serverbot2.workflow;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;
import io.mamish.serverbot2.workflow.model.ExecutionState;

@SuppressWarnings("unused")
public class LambdaHandler implements RequestHandler<String,Void> {

    private final Gson gson = new Gson();
    private final StepHandler stepHandler = new StepHandler();

    @Override
    public Void handleRequest(String payload, Context context) {

        ExecutionState executionState = gson.fromJson(payload, ExecutionState.class);

        switch (executionState.getTaskName()) {
            case CreateGameMetadata:
                stepHandler.createGameMetadata(executionState); break;
            case LockGame:
                stepHandler.lockGame(executionState); break;
            case CreateGameResources:
                stepHandler.createGameResources(executionState); break;
            case StartInstance:
                stepHandler.startInstance(executionState); break;
            case WaitInstanceReady:
                stepHandler.waitInstanceReady(executionState); break;
            case StartServer:
                stepHandler.startServer(executionState); break;
            case WaitServerStop:
                stepHandler.waitServerStop(executionState); break;
            case StopInstance:
                stepHandler.stopInstance(executionState); break;
            case DeleteGameResources:
                stepHandler.deleteGameResources(executionState); break;
        }

        return null;
    }

}
