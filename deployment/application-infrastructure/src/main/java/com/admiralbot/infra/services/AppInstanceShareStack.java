package com.admiralbot.infra.services;

import com.admiralbot.discordrelay.model.service.IDiscordService;
import com.admiralbot.gamemetadata.model.IGameMetadataService;
import com.admiralbot.infra.constructs.S3Artifact;
import com.admiralbot.infra.constructs.S3ArtifactProps;
import com.admiralbot.infra.deploy.ApplicationRegionalStage;
import com.admiralbot.infra.util.ManagedPolicies;
import com.admiralbot.infra.util.Permissions;
import com.admiralbot.infra.util.Util;
import com.admiralbot.networksecurity.model.INetworkSecurity;
import com.admiralbot.sharedconfig.AppInstanceConfig;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.iam.CfnInstanceProfile;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.s3.assets.Asset;

import java.util.List;

public class AppInstanceShareStack extends Stack {

    public AppInstanceShareStack(ApplicationRegionalStage parent, String id) {
        super(parent, id);

        // Distribute app daemon JAR file as an asset. Uses custom S3Artifact resource to copy to a separate, nicer
        // looking S3 bucket from the CDK asset staging bucket.

        String appDaemonJarPath = Util.mavenJarPath("app-daemon", "agents").toString();
        Asset appDaemonJarAsset = Asset.Builder.create(this, "AppDaemonJarAsset")
                .path(appDaemonJarPath)
                .build();
        new S3Artifact(this, "AppDaemonJarArtifact", new S3ArtifactProps(
                appDaemonJarAsset, parent.getCommonResources().getDeployedArtifactBucket(),
                "app-daemon", ".jar",
                AppInstanceConfig.APP_DAEMON_JAR_S3_URL.getName()
        ));

        // Same as above but for native artifact (to be distributed to new instances)

        String appDaemonLinuxBinPath = Util.codeBuildPath("agents", "app-daemon", "target", "deployzip",
                "bootstrap").toString();
        Asset appDaemonLinuxBinAsset = Asset.Builder.create(this, "AppDaemonLinuxBinAsset")
                .path(appDaemonLinuxBinPath)
                .build();
        new S3Artifact(this, "AppDaemonLinuxBinArtifact", new S3ArtifactProps(
                appDaemonLinuxBinAsset, parent.getCommonResources().getDeployedArtifactBucket(),
                "app-daemon-linux-bin", "",
                AppInstanceConfig.APP_DAEMON_LINUX_BIN_S3_URL.getName()
        ));

        // TODO: Need to come up with better permission scoping. This role is user-exposed so attack surface is
        // considerable. Needs to be scoped in both AWS and project service terms.

        Role commonRole = Role.Builder.create(this, "AppInstanceCommonRole")
                .assumedBy(new ServicePrincipal("ec2.amazonaws.com"))
                .managedPolicies(List.of(
                        ManagedPolicies.LOGS_FULL_ACCESS,
                        ManagedPolicies.SQS_FULL_ACCESS,
                        ManagedPolicies.S3_READ_ONLY_ACCESS,
                        ManagedPolicies.SSM_MANAGED_INSTANCE_CORE,
                        ManagedPolicies.STEP_FUNCTIONS_FULL_ACCESS
                )).build();

        Permissions.addConfigPathRead(this, commonRole,
                AppInstanceConfig.PATH_ALL);

        Permissions.addExecuteApi(this, commonRole,
                IDiscordService.class,
                IGameMetadataService.class,
                INetworkSecurity.class);

        CfnInstanceProfile commonInstanceProfile = CfnInstanceProfile.Builder.create(this, "AppInstanceCommonProfile")
                .instanceProfileName(AppInstanceConfig.COMMON_INSTANCE_PROFILE_NAME)
                .roles(List.of(commonRole.getRoleName()))
                .build();

    }

}
