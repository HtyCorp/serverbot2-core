package io.mamish.serverbot2.infra.services;

import io.mamish.serverbot2.infra.util.ManagedPolicies;
import io.mamish.serverbot2.infra.util.Util;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedconfig.GameMetadataConfig;
import io.mamish.serverbot2.sharedconfig.NetSecConfig;
import io.mamish.serverbot2.sharedconfig.WorkflowsConfig;
import io.mamish.serverbot2.sharedutil.IDUtils;
import io.mamish.serverbot2.workflow.model.Machines;
import io.mamish.serverbot2.workflow.model.Tasks;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.lambda.Alias;
import software.amazon.awscdk.services.lambda.IFunction;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.stepfunctions.*;
import software.amazon.awscdk.services.stepfunctions.tasks.LambdaInvoke;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkflowsStack extends Stack {

    public WorkflowsStack(Construct parent, String id) {
        super(parent, id);

        Role taskRole = Util.standardLambdaRole(this, "WorkflowFunctionRole", List.of(
                ManagedPolicies.EC2_FULL_ACCESS,
                ManagedPolicies.SQS_FULL_ACCESS,
                ManagedPolicies.ROUTE_53_FULL_ACCESS
        )).build();

        Util.addConfigPathReadPermissionToRole(this, taskRole, CommonConfig.PATH);

        Util.addLambdaInvokePermissionToRole(this, taskRole,
                GameMetadataConfig.FUNCTION_NAME,
                NetSecConfig.FUNCTION_NAME);

        taskRole.addToPolicy(PolicyStatement.Builder.create()
                .actions(List.of("iam:PassRole"))
                .resources(List.of("*"))
                .conditions(Map.of("StringEquals", Map.of("iam:PassedToService", "ec2.amazonaws.com")))
                .build());

        Alias taskFunctionAlias = Util.highMemJavaFunction(this, "WorkflowFunction",
                "workflow-service", "io.mamish.serverbot2.workflow.LambdaHandler",
                b -> b.timeout(Duration.seconds(WorkflowsConfig.STEP_LAMBDA_TIMEOUT_SECONDS)).role(taskRole));

        final long TIMEOUT_READY = WorkflowsConfig.NEW_INSTANCE_TIMEOUT_SECONDS;
        final long TIMEOUT_STOP = WorkflowsConfig.DAEMON_HEARTBEAT_TIMEOUT_SECONDS;

        StateMachine createGame = new StateMachineSteps(Machines.CreateGame, taskFunctionAlias)
                .sync(Tasks.CreateGameMetadata)
                .sync(Tasks.CreateGameResources)
                .callback(Tasks.WaitInstanceReady, TIMEOUT_READY)
                .sync(Tasks.InstanceReadyNotify)
                .callback(Tasks.WaitServerStop, TIMEOUT_STOP)
                .sync(Tasks.StopInstance)
                .endSuccess();

        StateMachine runGame = new StateMachineSteps(Machines.RunGame, taskFunctionAlias)
                .sync(Tasks.LockGame)
                .sync(Tasks.StartInstance)
                .callback(Tasks.WaitInstanceReady, TIMEOUT_READY)
                .sync(Tasks.InstanceReadyStartServer)
                .callback(Tasks.WaitServerStop, TIMEOUT_STOP)
                .sync(Tasks.StopInstance)
                .endSuccess();

        StateMachine editGame = new StateMachineSteps(Machines.EditGame, taskFunctionAlias)
                .sync(Tasks.LockGame)
                .sync(Tasks.StartInstance)
                .callback(Tasks.WaitInstanceReady, TIMEOUT_READY)
                .callback(Tasks.WaitServerStop, TIMEOUT_STOP)
                .sync(Tasks.StopInstance)
                .endSuccess();

        StateMachine deleteGame = new StateMachineSteps(Machines.DeleteGame, taskFunctionAlias)
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
        payloadMap.put("taskToken", JsonPath.getTaskToken());
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
                "requesterDiscordId", JsonPath.stringAt("$.requesterDiscordId"),
                "gameName", JsonPath.stringAt("$.gameName"),
                "initialMessageUuid", JsonPath.stringAt("$.initialMessageUuid"),
                "laterMessageUuid", JsonPath.stringAt("$.laterMessageUuid"),
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
