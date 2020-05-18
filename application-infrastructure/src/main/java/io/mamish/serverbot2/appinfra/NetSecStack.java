package io.mamish.serverbot2.appinfra;

import io.mamish.serverbot2.sharedconfig.NetSecConfig;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.kms.Key;
import software.amazon.awscdk.services.lambda.Function;

import java.util.List;

public class NetSecStack extends Stack {

    public NetSecStack(Construct parent, String id, StackProps props, CommonStack commonStack) {
        super(parent, id, props);

        Key userIdKey = Key.Builder.create(this, "UserIdKey")
                .trustAccountIdentities(true)
                .alias(NetSecConfig.KMS_ALIAS)
                .build();

        Role functionRole = Util.standardLambdaRole(this, "NetSecServiceLambda", List.of(
            Util.POLICY_EC2_FULL_ACCESS
        )).build();
        userIdKey.grant(functionRole);

        Function serviceFunction = Util.standardJavaFunction(this, "NetSecService", "network-security-service",
                "io.mamish.serverbot2.networksecurity.LambdaHandler", functionRole)
                .functionName(NetSecConfig.FUNCTION_NAME).build();

    }

}
