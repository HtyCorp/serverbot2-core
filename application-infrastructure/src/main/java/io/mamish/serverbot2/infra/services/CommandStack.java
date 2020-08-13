package io.mamish.serverbot2.infra.services;

import io.mamish.serverbot2.infra.util.Util;
import io.mamish.serverbot2.sharedconfig.CommandLambdaConfig;
import io.mamish.serverbot2.sharedconfig.GameMetadataConfig;
import io.mamish.serverbot2.sharedconfig.NetSecConfig;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Fn;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.iam.*;
import software.amazon.awscdk.services.lambda.Function;

import java.util.List;

public class CommandStack extends Stack {

    public CommandStack(Construct parent, String id) {
        super(parent, id);

        // Terminal access user (federation doesn't work when chaining from the function role)

        User ssmSessionUser = User.Builder.create(this, "SsmSessionUser")
                .userName("DiscordUser")
                .build();

        ssmSessionUser.addToPolicy(PolicyStatement.Builder.create()
                .actions(List.of("sts:GetFederationToken"))
                .resources(List.of("*"))
                .build());
        ssmSessionUser.addToPrincipalPolicy(PolicyStatement.Builder.create()
                .actions(List.of("ssm:StartSession"))
                .resources(List.of("*"))
                .build());

        CfnAccessKey accessKey = CfnAccessKey.Builder.create(this, "SsmSessionUserKey")
                .userName(ssmSessionUser.getUserName())
                .build();

        // CDK uses 'tokens' as placeholders for the eventual CFN-generated values: those are the actual strings we're
        // manipulating right now
        String fullAccessKeyAsToken = Fn.join(":", List.of(
                accessKey.getRef(),
                accessKey.getAttrSecretAccessKey()));

        Util.instantiateConfigSecret(this, "SsmSessionUserKeySecret",
                CommandLambdaConfig.TERMINAL_FEDERATION_ACCESS_KEY,
                fullAccessKeyAsToken);

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

    }
}