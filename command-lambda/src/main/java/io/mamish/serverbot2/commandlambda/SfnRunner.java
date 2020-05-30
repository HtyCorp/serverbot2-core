package io.mamish.serverbot2.commandlambda;

import com.google.gson.Gson;
import io.mamish.serverbot2.sharedutil.IDUtils;
import io.mamish.serverbot2.workflow.model.ExecutionState;
import io.mamish.serverbot2.workflow.model.Machines;
import software.amazon.awssdk.services.sfn.SfnClient;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SfnRunner {

    private final Gson gson = new Gson();
    private final SfnClient sfnClient = SfnClient.create();
    private final Map<Machines,String> stateMachineArns = generateNameToArnMap();

    public ExecutionState startExecution(Machines targetMachine, String gameName,
                                         String sourceMessageId, String sourceUserId) {
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
        sfnClient.sendTaskSuccess(r -> r.taskToken(taskToken).output("{}"));
    }

    private String generateExecutionId(String messageId, String userId) {
        return "t"+IDUtils.epochSeconds()
                + "-m"+messageId
                + "-u"+userId;
    }

    private Map<Machines,String> generateNameToArnMap() {
        Map<String,Machines> nameToEnumMap = Arrays.stream(Machines.values()).collect(Collectors.toMap(
                Machines::toString,
                Function.identity()
        ));
        return sfnClient.listStateMachines().stateMachines().stream()
                .filter(m -> nameToEnumMap.containsKey(m.name()))
                .collect(Collectors.toMap(m -> nameToEnumMap.get(m.name()), m -> m.stateMachineArn()));
    }

}
