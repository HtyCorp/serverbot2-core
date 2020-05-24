package io.mamish.serverbot2.appinfra;

import com.google.gson.Gson;
import io.mamish.serverbot2.sharedconfig.WorkflowsConfig;
import io.mamish.serverbot2.workflow.model.MachineNames;
import io.mamish.serverbot2.workflow.model.MachineTaskNames;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.IFunction;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.stepfunctions.*;
import software.amazon.awscdk.services.stepfunctions.tasks.LambdaInvoke;

import java.util.HashMap;
import java.util.Map;

public class WorkflowsStack extends Stack {

    private final Gson gson = new Gson();

    public WorkflowsStack(Construct parent, String id) {
        this(parent, id, null);
    }

    public WorkflowsStack(Construct parent, String id, StackProps props) {
        super(parent, id, props);

        Function taskFunc = Util.standardJavaFunction(this, "WorkflowFunction",
                "workflow-service", "io.mamish.serverbot2.workflow.LambdaHandler")
                .build();

        LambdaInvoke createGameMetadata = makeSynchronousTask(taskFunc, MachineTaskNames.CreateGameMetadata);
        LambdaInvoke lockGame = makeSynchronousTask(taskFunc, MachineTaskNames.LockGame);
        LambdaInvoke createGameResources = makeSynchronousTask(taskFunc, MachineTaskNames.CreateGameResources);
        LambdaInvoke startInstance = makeSynchronousTask(taskFunc, MachineTaskNames.StartInstance);
        LambdaInvoke waitInstanceReady = lambdaCallbackInvokeTask(taskFunc, MachineTaskNames.WaitInstanceReady,
                Duration.seconds(WorkflowsConfig.NEW_INSTANCE_TIMEOUT_SECONDS));
        LambdaInvoke startServer = makeSynchronousTask(taskFunc, MachineTaskNames.StartServer);
        LambdaInvoke waitServerStop = lambdaCallbackInvokeTask(taskFunc, MachineTaskNames.WaitServerStop,
                Duration.seconds(WorkflowsConfig.DAEMON_HEARTBEAT_TIMEOUT_SECONDS));
        LambdaInvoke stopInstance = makeSynchronousTask(taskFunc, MachineTaskNames.StopInstance);
        LambdaInvoke deleteGameResources = makeSynchronousTask(taskFunc, MachineTaskNames.DeleteGameResources);

        IChainable createGameTaskChain = createGameMetadata
                .next(lockGame)
                .next(createGameResources)
                .next(waitInstanceReady)
                .next(waitServerStop)
                .next(stopInstance);
        makeStateMachine(MachineNames.CreateGame, createGameTaskChain);

        IChainable runGameTaskChain = lockGame
                .next(startInstance)
                .next(waitInstanceReady)
                .next(startServer)
                .next(waitServerStop)
                .next(stopInstance);
        makeStateMachine(MachineNames.RunGame, runGameTaskChain);

        IChainable editGameTaskChain = lockGame
                .next(startInstance)
                .next(waitInstanceReady)
                .next(waitServerStop)
                .next(stopInstance);
        makeStateMachine(MachineNames.EditGame, editGameTaskChain);

        IChainable deleteGameTaskChain = lockGame
                .next(deleteGameResources);
        makeStateMachine(MachineNames.DeleteGame, deleteGameTaskChain);

    }

    private StateMachine makeStateMachine(MachineNames name, IChainable definition) {
        LogGroup logGroup = LogGroup.Builder.create(this, name+"Logs")
                .logGroupName("StateMachines/"+name)
                .build();

        LogOptions logOptions = LogOptions.builder()
                .destination(logGroup)
                .includeExecutionData(true)
                .level(LogLevel.ALL)
                .build();

        return StateMachine.Builder.create(this, name+"Machine")
                .logs(logOptions)
                .stateMachineType(StateMachineType.STANDARD)
                .stateMachineName(name.toString())
                .definition(definition)
                .build();
    }

    private LambdaInvoke makeSynchronousTask(IFunction function, MachineTaskNames name) {
        TaskInput payload = TaskInput.fromObject(basePayloadMap(name));
        return LambdaInvoke.Builder.create(this, name+"Task")
                .lambdaFunction(function)
                .resultPath("null")
                .payload(payload)
                .build();
    }

    private LambdaInvoke lambdaCallbackInvokeTask(IFunction function, MachineTaskNames name, Duration heartbeat) {
        Map<String,Object> payloadMap = new HashMap<>(basePayloadMap(name));
        payloadMap.put("taskToken", "$$.Task.Token");
        TaskInput payload = TaskInput.fromObject(payloadMap);

        return LambdaInvoke.Builder.create(this, name+"Task")
                .lambdaFunction(function)
                .resultPath("null")
                .payload(payload)
                .integrationPattern(IntegrationPattern.WAIT_FOR_TASK_TOKEN)
                .heartbeat(heartbeat)
                .build();
    }

    private Map<String,Object> basePayloadMap(MachineTaskNames name) {
        return Map.of(
                "requesterDiscordId", Data.stringAt("$.requesterDiscordId"),
                "gameName", Data.stringAt("$.gameName"),
                "randomId1", Data.stringAt("$.randomId1"),
                "randomId2", Data.stringAt("$.randomId2"),
                "taskName", name.toString()
        );
    }

}
