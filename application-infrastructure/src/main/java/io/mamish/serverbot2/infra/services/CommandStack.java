package io.mamish.serverbot2.infra.services;

import io.mamish.serverbot2.commandlambda.model.ICommandService;
import io.mamish.serverbot2.gamemetadata.model.IGameMetadataService;
import io.mamish.serverbot2.infra.constructs.EcsMicroservice;
import io.mamish.serverbot2.infra.constructs.ServiceApi;
import io.mamish.serverbot2.infra.deploy.ApplicationStage;
import io.mamish.serverbot2.infra.util.ManagedPolicies;
import io.mamish.serverbot2.infra.util.Permissions;
import io.mamish.serverbot2.infra.util.Util;
import io.mamish.serverbot2.networksecurity.model.INetworkSecurity;
import io.mamish.serverbot2.sharedconfig.AppInstanceConfig;
import io.mamish.serverbot2.sharedconfig.CommandLambdaConfig;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import software.amazon.awscdk.core.Fn;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.iam.CfnAccessKey;
import software.amazon.awscdk.services.iam.CfnUser;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.User;

import java.util.List;
import java.util.Objects;

public class CommandStack extends Stack {

    public CommandStack(ApplicationStage parent, String id) {
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
        CfnUser sessionUserCfn = ((CfnUser)ssmSessionUser.getNode().getDefaultChild());
        Objects.requireNonNull(sessionUserCfn).getTags().setTag("SSMSessionRunAs", AppInstanceConfig.MANAGED_OS_USER_NAME);

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

        // Service

        EcsMicroservice service = new EcsMicroservice(this, "Service", parent, "command-service");

        Permissions.addManagedPoliciesToRole(service.getTaskRole(),
                ManagedPolicies.STEP_FUNCTIONS_FULL_ACCESS,
                ManagedPolicies.SQS_FULL_ACCESS,
                ManagedPolicies.EC2_FULL_ACCESS
        );
        Permissions.addExecuteApi(this, service,
                IGameMetadataService.class,
                INetworkSecurity.class
        );
        Permissions.addConfigPathRead(this, service,
                CommandLambdaConfig.PATH,
                CommonConfig.PATH
        );
        Permissions.addFullExecuteApi(this, service);

        ServiceApi api = new ServiceApi(this, "Api", parent, ICommandService.class);
        api.addEcsRoute(ICommandService.class, service);

    }
}
