package io.mamish.serverbot2.infra.core;

import io.mamish.serverbot2.infra.util.Policies;
import io.mamish.serverbot2.infra.util.Util;
import io.mamish.serverbot2.sharedconfig.AppInstanceConfig;
import io.mamish.serverbot2.sharedconfig.GameMetadataConfig;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.iam.CfnInstanceProfile;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.s3.Bucket;

import java.util.List;

public class AppInstanceShareStack extends Stack {

    public AppInstanceShareStack(Construct parent, String id, StackProps props) {
        super(parent, id, props);

        Bucket deployedArtifactBucket = Bucket.Builder.create(this, "DeployedArtifactBucket")
                .build();

        Util.instantiateConfigSsmParameter(this, "ArtifactBucketNameParam",
                AppInstanceConfig.ARTIFACT_BUCKET_NAME, deployedArtifactBucket.getBucketName());

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

        Util.addLambdaInvokePermissionToRole(this, commonRole, GameMetadataConfig.FUNCTION_NAME);

        CfnInstanceProfile commonInstanceProfile = CfnInstanceProfile.Builder.create(this, "AppInstanceCommonProfile")
                .instanceProfileName(AppInstanceConfig.COMMON_INSTANCE_PROFILE_NAME)
                .roles(List.of(commonRole.getRoleName()))
                .build();

    }

}
