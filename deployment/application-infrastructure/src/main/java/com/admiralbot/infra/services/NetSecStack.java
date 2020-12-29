package com.admiralbot.infra.services;

import com.admiralbot.infra.constructs.EcsMicroservice;
import com.admiralbot.infra.constructs.ServiceApi;
import com.admiralbot.infra.deploy.ApplicationRegionalStage;
import com.admiralbot.infra.util.ManagedPolicies;
import com.admiralbot.infra.util.Permissions;
import com.admiralbot.networksecurity.model.INetworkSecurity;
import com.admiralbot.sharedconfig.CommonConfig;
import com.admiralbot.sharedconfig.NetSecConfig;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.ec2.CfnPrefixList;
import software.amazon.awscdk.services.ec2.Peer;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.iam.Role;

public class NetSecStack extends Stack {

    public NetSecStack(ApplicationRegionalStage parent, String id) {
        super(parent, id);

        CfnPrefixList userIpList = CfnPrefixList.Builder.create(this, "DiscordUserIpPrefixList")
                .addressFamily("IPv4")
                .prefixListName(NetSecConfig.USER_IP_PREFIX_LIST_NAME)
                .maxEntries(parent.getEnv().getPrefixListCapacity())
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

        EcsMicroservice service = new EcsMicroservice(this, "Service", parent, "network-security-service");

        Role taskRole = service.getTaskRole();
        Permissions.addManagedPoliciesToRole(taskRole, ManagedPolicies.EC2_FULL_ACCESS);
        Permissions.addConfigPathRead(this, taskRole, CommonConfig.PATH, NetSecConfig.PATH_PUBLIC);
        Permissions.addFullExecuteApi(this, taskRole);
        parent.getCommonResources().getNetSecKmsKey().grant(taskRole, "kms:Encrypt", "kms:Decrypt", "kms:GenerateDataKey");

        ServiceApi api = new ServiceApi(this, "Api", parent, INetworkSecurity.class);
        api.addEcsRoute(INetworkSecurity.class, service);

    }

}
