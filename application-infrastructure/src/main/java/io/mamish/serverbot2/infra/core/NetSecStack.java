package io.mamish.serverbot2.infra.core;

import io.mamish.serverbot2.infra.util.Policies;
import io.mamish.serverbot2.infra.util.Util;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedconfig.NetSecConfig;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.lambda.Function;

import java.util.List;

public class NetSecStack extends Stack {

    public NetSecStack(Construct parent, String id, StackProps props, CommonStack commonStack) {
        super(parent, id, props);

        Role functionRole = Util.standardLambdaRole(this, "NetSecServiceLambda", List.of(
                Policies.EC2_FULL_ACCESS,
                Policies.LOGS_FULL_ACCESS
        )).build();
        Util.addConfigPathReadPermissionToRole(this, functionRole, CommonConfig.PATH);

        commonStack.getNetSecKmsKey().grant(functionRole, "kms:Encrypt", "kms:Decrypt", "kms:GenerateDataKey");

        Function serviceFunction = Util.standardJavaFunction(this, "NetSecService", "network-security-service",
                "io.mamish.serverbot2.networksecurity.LambdaHandler", functionRole)
                .functionName(NetSecConfig.FUNCTION_NAME).build();

    }

}
