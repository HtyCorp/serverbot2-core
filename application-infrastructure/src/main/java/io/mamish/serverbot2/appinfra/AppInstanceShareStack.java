package io.mamish.serverbot2.appinfra;

import io.mamish.serverbot2.sharedconfig.AppInstanceConfig;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedconfig.GameMetadataConfig;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.iam.CfnInstanceProfile;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;

import java.util.List;

public class AppInstanceShareStack extends Stack {

    public AppInstanceShareStack(Construct parent, String id, StackProps props) {
        super(parent, id, props);

        Role commonRole = Role.Builder.create(this, "AppInstanceCommonRole")
                .assumedBy(new ServicePrincipal("ec2.amazonaws.com"))
                .managedPolicies(List.of(
                        Util.POLICY_SQS_FULL_ACCESS,
                        Util.POLICY_S3_READ_ONLY_ACCESS
                )).build();

        Util.addConfigPathReadPermissionToRole(this, commonRole, CommonConfig.PATH);
        Util.addLambdaInvokePermissionToRole(this, commonRole, GameMetadataConfig.FUNCTION_NAME);

        CfnInstanceProfile commonInstanceProfile = CfnInstanceProfile.Builder.create(this, "AppInstanceCommonProfile")
                .instanceProfileName(AppInstanceConfig.COMMON_INSTANCE_PROFILE_NAME)
                .roles(List.of(commonRole.getRoleName()))
                .build();



    }

}
