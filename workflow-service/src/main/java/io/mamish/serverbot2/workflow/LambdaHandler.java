package io.mamish.serverbot2.workflow;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.google.gson.Gson;
import io.mamish.serverbot2.workflow.model.ExecutionState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

@SuppressWarnings("unused")
public class LambdaHandler implements RequestStreamHandler {

    private final Logger logger = LogManager.getLogger(LambdaHandler.class);

    private final Gson gson = new Gson();
    private final StepHandler stepHandler = new StepHandler();

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {

        String payload = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        logger.info("Execution payload:\n" + payload);
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

        String emptyOutput = "{}";
        outputStream.write(emptyOutput.getBytes(StandardCharsets.UTF_8));
    }

}
