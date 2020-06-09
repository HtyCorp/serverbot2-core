package io.mamish.serverbot2.infra.core;

import io.mamish.serverbot2.infra.util.Util;
import io.mamish.serverbot2.sharedconfig.CommandLambdaConfig;
import io.mamish.serverbot2.sharedconfig.GameMetadataConfig;
import io.mamish.serverbot2.sharedconfig.NetSecConfig;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.lambda.Function;

import java.util.List;

public class CommandStack extends Stack {

    public CommandStack(Construct parent, String id, StackProps props) {
        super(parent, id, props);

        // Service function

        Role functionRole = Util.standardLambdaRole(this, "CommandFunctionExecutionRole", List.of(
                ManagedPolicy.fromAwsManagedPolicyName("AWSStepFunctionsFullAccess"),
                ManagedPolicy.fromAwsManagedPolicyName("AmazonSQSFullAccess")
        )).build();

        Util.addLambdaInvokePermissionToRole(this, functionRole,
                GameMetadataConfig.FUNCTION_NAME,
                NetSecConfig.FUNCTION_NAME);

        Util.addConfigPathReadPermissionToRole(this, functionRole, CommandLambdaConfig.PATH);

        Function serviceFunction = Util.standardJavaFunction(this, "CommandService", "command-lambda",
                "io.mamish.serverbot2.commandlambda.LambdaHandler", functionRole)
                .functionName(CommandLambdaConfig.FUNCTION_NAME).build();

        // Terminal access role

        Role sessionRole = Role.Builder.create(this, "SsmSessionRole")
                .assumedBy(functionRole)
                .maxSessionDuration(Duration.hours(CommandLambdaConfig.TERMINAL_SESSION_ROLE_DURATION_HOURS))
                .build();

        sessionRole.addToPrincipalPolicy(PolicyStatement.Builder.create()
                .actions(List.of("ssm:StartSession"))
                .resources(List.of("*"))
                .build());

        Util.instantiateConfigSsmParameter(this, "SsmSessionRoleArnParam",
                CommandLambdaConfig.TERMINAL_SESSION_ROLE_ARN, sessionRole.getRoleArn());

    }
}
