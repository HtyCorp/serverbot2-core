package io.mamish.serverbot2.infra.services;

import io.mamish.serverbot2.infra.deploy.ApplicationStage;
import io.mamish.serverbot2.infra.util.ManagedPolicies;
import io.mamish.serverbot2.infra.util.Util;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedconfig.NetSecConfig;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.ec2.CfnPrefixList;
import software.amazon.awscdk.services.ec2.Peer;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.iam.Role;

import java.util.List;

public class NetSecStack extends Stack {

    public NetSecStack(ApplicationStage parent, String id) {
        super(parent, id);

        CfnPrefixList userIpList = CfnPrefixList.Builder.create(this, "DiscordUserIpPrefixList")
                .addressFamily("IPv4")
                .prefixListName(NetSecConfig.USER_IP_PREFIX_LIST_NAME)
                .maxEntries(NetSecConfig.MAX_USER_IP_ADDRESSES)
                .build();

        SecurityGroup commonGroup = SecurityGroup.Builder.create(this, "AppInstanceCommonGroup")
                .vpc(parent.getCommonResources().getApplicationVpc())
                .allowAllOutbound(false)
                .securityGroupName(NetSecConfig.APP_INSTANCE_COMMON_SG_NAME)
                .description("Group for common ports/protocols on application instances")
                .build();
        commonGroup.addIngressRule(Peer.prefixList(userIpList.getAttrPrefixListId()),
                Port.tcp(NetSecConfig.APP_INSTANCE_SFTP_PORT),
                "Custom SFTP port for Apache SSHD / SFTP");

        Role functionRole = Util.standardLambdaRole(this, "NetSecServiceLambda", List.of(
                ManagedPolicies.EC2_FULL_ACCESS,
                ManagedPolicies.LOGS_FULL_ACCESS
        )).build();

        Util.addConfigPathReadPermissionToRole(this, functionRole, CommonConfig.PATH);
        Util.addFullExecuteApiPermissionToRole(this, functionRole);

        parent.getCommonResources().getNetSecKmsKey().grant(functionRole, "kms:Encrypt", "kms:Decrypt", "kms:GenerateDataKey");

        Util.highMemJavaFunction(this, "NetSecService", "network-security-service",
                "io.mamish.serverbot2.networksecurity.LambdaHandler",
                b -> b.functionName(NetSecConfig.FUNCTION_NAME).role(functionRole));

    }

}
