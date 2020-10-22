package io.mamish.serverbot2.infra.services;

import io.mamish.serverbot2.infra.util.ManagedPolicies;
import io.mamish.serverbot2.infra.util.Util;
import io.mamish.serverbot2.sharedconfig.*;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Fn;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.iam.*;

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

        // User construct doesn't expose tags: need to use backing CFN resource directly
        ((CfnUser)ssmSessionUser.getNode().getDefaultChild()).getTags().setTag("SSMSessionRunAs",
                AppInstanceConfig.MANAGED_OS_USER_NAME);

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
                ManagedPolicies.STEP_FUNCTIONS_FULL_ACCESS,
                ManagedPolicies.SQS_FULL_ACCESS,
                ManagedPolicies.EC2_READ_ONLY_ACCESS
        )).build();

        Util.addLambdaInvokePermissionToRole(this, functionRole,
                GameMetadataConfig.FUNCTION_NAME,
                NetSecConfig.FUNCTION_NAME);
        Util.addConfigPathReadPermissionToRole(this, functionRole,
                CommandLambdaConfig.PATH,
                CommonConfig.PATH);
        Util.addFullExecuteApiPermissionToRole(this, functionRole);

        Util.highMemJavaFunction(this, "CommandService", "command-lambda",
                "io.mamish.serverbot2.commandlambda.LambdaHandler",
                b -> b.functionName(CommandLambdaConfig.FUNCTION_NAME).role(functionRole));

    }
}
