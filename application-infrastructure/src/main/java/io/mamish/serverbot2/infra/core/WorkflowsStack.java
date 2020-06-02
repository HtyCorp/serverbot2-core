package io.mamish.serverbot2.infra.core;

import io.mamish.serverbot2.infra.util.Util;
import io.mamish.serverbot2.sharedconfig.WorkflowsConfig;
import io.mamish.serverbot2.sharedutil.IDUtils;
import io.mamish.serverbot2.workflow.model.Machines;
import io.mamish.serverbot2.workflow.model.Tasks;
import software.amazon.awscdk.core.*;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.IFunction;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.stepfunctions.*;
import software.amazon.awscdk.services.stepfunctions.tasks.LambdaInvoke;

import java.util.HashMap;
import java.util.Map;

public class WorkflowsStack extends Stack {

    public WorkflowsStack(Construct parent, String id, StackProps props) {
        super(parent, id, props);

        Function taskFunction = Util.standardJavaFunction(this, "WorkflowFunction",
                "workflow-service", "io.mamish.serverbot2.workflow.LambdaHandler")
                .build();

        final long TIMEOUT_READY = WorkflowsConfig.NEW_INSTANCE_TIMEOUT_SECONDS;
        final long TIMEOUT_STOP = WorkflowsConfig.DAEMON_HEARTBEAT_TIMEOUT_SECONDS;

        StateMachine createGame = new StateMachineSteps(Machines.CreateGame, taskFunction)
                .sync(Tasks.CreateGameMetadata)
                .sync(Tasks.CreateGameResources)
                .callback(Tasks.WaitInstanceReady, TIMEOUT_READY)
                .callback(Tasks.WaitServerStop, TIMEOUT_STOP)
                .sync(Tasks.StopInstance)
                .endSuccess();

        StateMachine runGame = new StateMachineSteps(Machines.RunGame, taskFunction)
                .sync(Tasks.LockGame)
                .sync(Tasks.StartInstance)
                .callback(Tasks.WaitInstanceReady, TIMEOUT_READY)
                .sync(Tasks.StartServer)
                .callback(Tasks.WaitServerStop, TIMEOUT_STOP)
                .sync(Tasks.StopInstance)
                .endSuccess();

        StateMachine editGame = new StateMachineSteps(Machines.EditGame, taskFunction)
                .sync(Tasks.LockGame)
                .sync(Tasks.StartInstance)
                .callback(Tasks.WaitInstanceReady, TIMEOUT_READY)
                .callback(Tasks.WaitServerStop, TIMEOUT_STOP)
                .sync(Tasks.StopInstance)
                .endSuccess();

        StateMachine deleteGame = new StateMachineSteps(Machines.DeleteGame, taskFunction)
                .sync(Tasks.LockGame)
                .sync(Tasks.DeleteGameResources)
                .endSuccess();

    }

    /* This is necessary because tasks can't be reused between state machines - I tried.
     * This holds the necessary state to reconstruct tasks in each state machine with the same chaining convenience.
     */
    private class StateMachineSteps {

        private final Machines machineName;
        private final IFunction taskFunction;

        private Chain outputTaskChain;

        public StateMachineSteps(Machines machineName, IFunction taskFunction) {
            this.machineName = machineName;
            this.taskFunction = taskFunction;
        }

        StateMachineSteps sync(Tasks taskName) {
            addToChain(makeSynchronousTask(machineName, taskFunction, taskName));
            return this;
        }

        StateMachineSteps callback(Tasks taskName, long timeoutSeconds) {
            addToChain(makeCallbackTask(machineName, taskFunction, taskName, Duration.seconds(timeoutSeconds)));
            return this;
        }

        StateMachine endSuccess() {
            Succeed succeed = Succeed.Builder.create(WorkflowsStack.this,
                    IDUtils.kebab(machineName, "EndSucceed")).build();
            addToChain(succeed);
            return makeStateMachine(machineName, outputTaskChain);
        }

        private void addToChain(State nextTask) {
            if (outputTaskChain == null) {
                outputTaskChain = Chain.start(nextTask);
            } else {
                outputTaskChain = outputTaskChain.next(nextTask);
            }
        }

    }

    private LambdaInvoke makeSynchronousTask(Machines machine, IFunction function, Tasks task) {
        TaskInput payload = TaskInput.fromObject(basePayloadMap(task));

        String scopedId = IDUtils.kebab(machine, "Task", task);
        return LambdaInvoke.Builder.create(this, scopedId)
                .lambdaFunction(function)
                .resultPath("DISCARD")
                .payload(payload)
                .integrationPattern(IntegrationPattern.REQUEST_RESPONSE)
                .build();
    }

    private LambdaInvoke makeCallbackTask(Machines machine, IFunction function, Tasks task, Duration heartbeat) {
        Map<String,Object> payloadMap = new HashMap<>(basePayloadMap(task));
        payloadMap.put("taskToken", Context.getTaskToken());
        TaskInput payload = TaskInput.fromObject(payloadMap);

        String scopedId = IDUtils.kebab(machine, "Task", task);
        return LambdaInvoke.Builder.create(this, scopedId)
                .lambdaFunction(function)
                .resultPath("DISCARD")
                .payload(payload)
                .integrationPattern(IntegrationPattern.WAIT_FOR_TASK_TOKEN)
                .heartbeat(heartbeat)
                .build();
    }

    private Map<String,Object> basePayloadMap(Tasks task) {
        return Map.of(
                "requesterDiscordId", Data.stringAt("$.requesterDiscordId"),
                "gameName", Data.stringAt("$.gameName"),
                "initialMessageUuid", Data.stringAt("$.initialMessageUuid"),
                "laterMessageUuid", Data.stringAt("$.laterMessageUuid"),
                "taskName", task.toString()
        );
    }

    private StateMachine makeStateMachine(Machines machine, IChainable definition) {
        LogGroup logGroup = LogGroup.Builder.create(this, machine+"Logs")
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        LogOptions logOptions = LogOptions.builder()
                .destination(logGroup)
                .includeExecutionData(true)
                .level(LogLevel.ALL)
                .build();

        return StateMachine.Builder.create(this, machine+"Machine")
                .logs(logOptions)
                .stateMachineType(StateMachineType.STANDARD)
                .stateMachineName(machine.toString())
                .definition(definition)
                .build();
    }

}
