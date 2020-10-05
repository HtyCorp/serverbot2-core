package io.mamish.serverbot2.infra.services;

import io.mamish.serverbot2.infra.util.Policies;
import io.mamish.serverbot2.infra.util.Util;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedconfig.NetSecConfig;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.ec2.CfnPrefixList;
import software.amazon.awscdk.services.iam.Role;

import java.util.List;

public class NetSecStack extends Stack {

    public NetSecStack(Construct parent, String id, CommonStack commonStack) {
        super(parent, id);

        CfnPrefixList.Builder.create(this, "DiscordUserIpPrefixList")
                .addressFamily("IPv4")
                .prefixListName(NetSecConfig.USER_IP_PREFIX_LIST_NAME)
                .maxEntries(NetSecConfig.MAX_USER_IP_ADDRESSES)
                .build();

        Role functionRole = Util.standardLambdaRole(this, "NetSecServiceLambda", List.of(
                Policies.EC2_FULL_ACCESS,
                Policies.LOGS_FULL_ACCESS
        )).build();

        Util.addConfigPathReadPermissionToRole(this, functionRole, CommonConfig.PATH);
        Util.addFullExecuteApiPermissionToRole(this, functionRole);

        commonStack.getNetSecKmsKey().grant(functionRole, "kms:Encrypt", "kms:Decrypt", "kms:GenerateDataKey");

        Util.highMemJavaFunction(this, "NetSecService", "network-security-service",
                "io.mamish.serverbot2.networksecurity.LambdaHandler",
                b -> b.functionName(NetSecConfig.FUNCTION_NAME).role(functionRole));

    }

}
