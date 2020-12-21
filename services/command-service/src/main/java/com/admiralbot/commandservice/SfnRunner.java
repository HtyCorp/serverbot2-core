package com.admiralbot.commandservice;

import com.admiralbot.sharedutil.IDUtils;
import com.admiralbot.sharedutil.SdkUtils;
import com.admiralbot.workflows.model.ExecutionState;
import com.admiralbot.workflows.model.Machines;
import com.google.gson.Gson;
import software.amazon.awssdk.services.sfn.SfnClient;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SfnRunner {

    private final Gson gson = new Gson();

    private SfnClient sfnClient;
    private Map<Machines,String> stateMachineArns;

    // To reduce cold starts, this is deliberately deferred since most commands don't require it.
    private void ensureClientInitialised() {
        if (sfnClient == null) {
            sfnClient = SdkUtils.client(SfnClient.builder());
            Map<String,Machines> nameToEnumMap = Arrays.stream(Machines.values()).collect(Collectors.toMap(
                    Machines::toString,
                    Function.identity()
            ));
            stateMachineArns = sfnClient.listStateMachines().stateMachines().stream()
                    .filter(m -> nameToEnumMap.containsKey(m.name()))
                    .collect(Collectors.toMap(m -> nameToEnumMap.get(m.name()), m -> m.stateMachineArn()));
        }
    }

    public ExecutionState startExecution(Machines targetMachine, String gameName,
                                         String sourceMessageId, String sourceUserId) {

        ensureClientInitialised();

        ExecutionState inputState = new ExecutionState(
                sourceUserId,
                gameName,
                IDUtils.randomUUID(),
                IDUtils.randomUUID()
        );
        String inputStateJson = gson.toJson(inputState);
        String executionId = generateExecutionId(sourceMessageId, sourceUserId);
        sfnClient.startExecution(r -> r.stateMachineArn(stateMachineArns.get(targetMachine))
                .name(executionId)
                .input(inputStateJson));

        return inputState;
    }

    public void completeTask(String taskToken) {
        ensureClientInitialised();
        sfnClient.sendTaskSuccess(r -> r.taskToken(taskToken).output("{}"));
    }

    private String generateExecutionId(String messageId, String userId) {
        return "t"+IDUtils.epochSeconds()
                + "-m"+messageId
                + "-u"+userId;
    }

}
