package io.mamish.serverbot2.infra.services;

import io.mamish.serverbot2.infra.util.Policies;
import io.mamish.serverbot2.infra.util.Util;
import io.mamish.serverbot2.sharedconfig.AppInstanceConfig;
import io.mamish.serverbot2.sharedconfig.GameMetadataConfig;
import io.mamish.serverbot2.sharedconfig.NetSecConfig;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.iam.CfnInstanceProfile;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.s3.assets.Asset;

import java.util.List;

public class AppInstanceShareStack extends Stack {

    public AppInstanceShareStack(Construct parent, String id) {
        super(parent, id);

        // Distribute app daemon JAR file as an asset. Create an SSM param with the S3 URL so app instances can fetch.

        String appDaemonJarPath = System.getenv("CODEBUILD_SRC_DIR") + "/gen/app-daemon/app-daemon.jar";
        Asset appDaemonJarAsset = Asset.Builder.create(this, "AppDaemonJarAsset")
                .path(appDaemonJarPath)
                .build();
        Util.instantiateConfigSsmParameter(this, "AppDaemonJarParam",
                AppInstanceConfig.APP_DAEMON_JAR_S3_URL, appDaemonJarAsset.getS3ObjectUrl());

        // TODO: Need to come up with better permission scoping. This role is user-exposed so attack surface is
        // considerable. Needs to be scoped in both AWS and project service terms.

        Role commonRole = Role.Builder.create(this, "AppInstanceCommonRole")
                .assumedBy(new ServicePrincipal("ec2.amazonaws.com"))
                .managedPolicies(List.of(
                        Policies.LOGS_FULL_ACCESS,
                        Policies.SQS_FULL_ACCESS,
                        Policies.S3_READ_ONLY_ACCESS,
                        Policies.SSM_MANAGED_INSTANCE_CORE,
                        Policies.STEP_FUNCTIONS_FULL_ACCESS
                )).build();

        Util.addConfigPathReadPermissionToRole(this, commonRole, AppInstanceConfig.PATH_ALL);

        Util.addLambdaInvokePermissionToRole(this, commonRole,
                GameMetadataConfig.FUNCTION_NAME,
                NetSecConfig.FUNCTION_NAME);

        CfnInstanceProfile commonInstanceProfile = CfnInstanceProfile.Builder.create(this, "AppInstanceCommonProfile")
                .instanceProfileName(AppInstanceConfig.COMMON_INSTANCE_PROFILE_NAME)
                .roles(List.of(commonRole.getRoleName()))
                .build();

    }

}
