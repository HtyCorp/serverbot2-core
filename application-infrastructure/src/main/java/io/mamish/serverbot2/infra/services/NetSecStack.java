package io.mamish.serverbot2.infra.services;

import io.mamish.serverbot2.infra.constructs.EcsMicroservice;
import io.mamish.serverbot2.infra.constructs.ServiceApi;
import io.mamish.serverbot2.infra.deploy.ApplicationStage;
import io.mamish.serverbot2.infra.util.ManagedPolicies;
import io.mamish.serverbot2.infra.util.Util;
import io.mamish.serverbot2.networksecurity.model.INetworkSecurity;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedconfig.NetSecConfig;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.ec2.CfnPrefixList;
import software.amazon.awscdk.services.ec2.Peer;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.iam.Role;

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

        EcsMicroservice service = new EcsMicroservice(this, "Service", parent, "network-security-service");

        Role taskRole = service.getTaskRole();
        Util.addManagedPoliciesToRole(taskRole, ManagedPolicies.EC2_FULL_ACCESS);
        Util.addConfigPathReadPermission(this, taskRole, CommonConfig.PATH);
        Util.addFullExecuteApiPermission(this, taskRole);
        parent.getCommonResources().getNetSecKmsKey().grant(taskRole, "kms:Encrypt", "kms:Decrypt", "kms:GenerateDataKey");

        ServiceApi api = new ServiceApi(this, "Api", parent, INetworkSecurity.class);
        api.addEcsRoute(INetworkSecurity.class, service);

    }

}
