package io.mamish.serverbot2.infra.services;

import io.mamish.serverbot2.infra.deploy.ApplicationEnv;
import io.mamish.serverbot2.infra.deploy.ApplicationStage;
import io.mamish.serverbot2.infra.util.Util;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedconfig.DiscordConfig;
import io.mamish.serverbot2.sharedconfig.NetSecConfig;
import io.mamish.serverbot2.sharedutil.IDUtils;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.core.SecretValue;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.apigatewayv2.VpcLink;
import software.amazon.awscdk.services.certificatemanager.CertificateValidation;
import software.amazon.awscdk.services.certificatemanager.DnsValidatedCertificate;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.kms.Key;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.route53.HostedZone;
import software.amazon.awscdk.services.route53.HostedZoneAttributes;
import software.amazon.awscdk.services.route53.IHostedZone;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.servicediscovery.PrivateDnsNamespace;

import java.util.List;
import java.util.Map;

public class CommonStack extends Stack {

    private final Bucket deployedArtifactBucket;
    private final Vpc serviceVpc;
    private final Vpc applicationVpc;
    private final IHostedZone systemRootHostedZone;
    private final IHostedZone appRootHostedZone;
    private final DnsValidatedCertificate systemWildcardCertificate;
    private final DnsValidatedCertificate systemServicesWildcardCertificate;
    private final DnsValidatedCertificate appWildcardCertificate;
    private final Key netSecKmsKey;
    private final VpcLink apiVpcLink;
    private final PrivateDnsNamespace apiVpcNamespace;

    public Bucket getDeployedArtifactBucket() {
        return deployedArtifactBucket;
    }

    public Vpc getServiceVpc() {
        return serviceVpc;
    }

    public Vpc getApplicationVpc() {
        return applicationVpc;
    }

    public IHostedZone getSystemRootHostedZone() {
        return systemRootHostedZone;
    }

    public IHostedZone getAppRootHostedZone() {
        return appRootHostedZone;
    }

    public DnsValidatedCertificate getSystemWildcardCertificate() {
        return systemWildcardCertificate;
    }

    public DnsValidatedCertificate getSystemServicesWildcardCertificate() {
        return systemServicesWildcardCertificate;
    }

    public DnsValidatedCertificate getAppWildcardCertificate() {
        return appWildcardCertificate;
    }

    public Key getNetSecKmsKey() {
        return netSecKmsKey;
    }

    public VpcLink getApiVpcLink() {
        return apiVpcLink;
    }

    public PrivateDnsNamespace getApiVpcNamespace() {
        return apiVpcNamespace;
    }

    public CommonStack(ApplicationStage parent, String id) {
        super(parent, id);

        ApplicationEnv env = parent.getEnv();

        SecretValue discordApiTokenSource = SecretValue.secretsManager(env.getDiscordApiTokenSourceSecretArn());
        Util.instantiateConfigSecret(this, "DiscordApiTokenSecret",
                DiscordConfig.API_TOKEN, discordApiTokenSource.toString());

        Util.instantiateConfigSsmParameter(this, "SystemRootDomainNameParam",
                CommonConfig.SYSTEM_ROOT_DOMAIN_NAME, env.getSystemRootDomainName());
        Util.instantiateConfigSsmParameter(this, "SystemRootDomainZoneIdParam",
                CommonConfig.SYSTEM_ROOT_DOMAIN_ZONE_ID, env.getSystemRootDomainZoneId());
        Util.instantiateConfigSsmParameter(this, "AppRootDomainNameParam",
                CommonConfig.APP_ROOT_DOMAIN_NAME, env.getAppRootDomainName());
        Util.instantiateConfigSsmParameter(this, "AppRootDomainZoneIdParam",
                CommonConfig.APP_ROOT_DOMAIN_ZONE_ID, env.getAppRootDomainZoneId());
        Util.instantiateConfigSsmParameter(this, "ChannelIdWelcomeParam",
                DiscordConfig.CHANNEL_ID_WELCOME, env.getDiscordRelayChannelIdWelcome());
        Util.instantiateConfigSsmParameter(this, "ChannelIdMainParam",
                DiscordConfig.CHANNEL_ID_SERVERS, env.getDiscordRelayChannelIdMain());
        Util.instantiateConfigSsmParameter(this, "ChannelIdAdminParam",
                DiscordConfig.CHANNEL_ID_ADMIN, env.getDiscordRelayChannelIdAdmin());
        Util.instantiateConfigSsmParameter(this, "ChannelIdDebugParam",
                DiscordConfig.CHANNEL_ID_DEBUG, env.getDiscordRelayChannelIdDebug());
        Util.instantiateConfigSsmParameter(this, "RoleIdMainParam",
                DiscordConfig.CHANNEL_ROLE_SERVERS, env.getDiscordRelayRoleIdMain());
        Util.instantiateConfigSsmParameter(this, "RoleIdDebugParam",
                DiscordConfig.CHANNEL_ROLE_DEBUG, env.getDiscordRelayRoleIdDebug());

        deployedArtifactBucket = Bucket.Builder.create(this, "DeployedArtifactBucket")
                .bucketName(env.getArtifactBucketName())
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        List<SubnetConfiguration> singlePublicSubnet = List.of(SubnetConfiguration.builder()
                .name("main")
                .subnetType(SubnetType.PUBLIC)
                .cidrMask(24)
                .build());

        serviceVpc = Vpc.Builder.create(this, "ServiceVpc")
                .cidr(CommonConfig.APPLICATION_VPC_CIDR)
                .maxAzs(3)
                .subnetConfiguration(singlePublicSubnet)
                .build();

        LogGroup appFlowLogsGroup = LogGroup.Builder.create(this, "AppFlowLogsGroup")
                .logGroupName(CommonConfig.APPLICATION_VPC_FLOW_LOGS_GROUP_NAME)
                .removalPolicy(RemovalPolicy.DESTROY)
                .retention(RetentionDays.ONE_WEEK)
                .build();

        FlowLogOptions appFlowLogsOptions = FlowLogOptions.builder()
                .trafficType(FlowLogTrafficType.ACCEPT) // ALL has too much probe/scan traffic
                .destination(FlowLogDestination.toCloudWatchLogs(appFlowLogsGroup))
                .build();

        applicationVpc = Vpc.Builder.create(this, "ApplicationVpc")
                .cidr(CommonConfig.APPLICATION_VPC_CIDR)
                .flowLogs(Map.of("default", appFlowLogsOptions))
                .maxAzs(3)
                .subnetConfiguration(singlePublicSubnet)
                .build();

        Util.instantiateConfigSsmParameter(this, "AppVpcIdParameter",
                        CommonConfig.APPLICATION_VPC_ID, applicationVpc.getVpcId());

        HostedZoneAttributes existingSystemZoneAttributes = HostedZoneAttributes.builder()
                .hostedZoneId(env.getSystemRootDomainZoneId())
                .zoneName(env.getSystemRootDomainName())
                .build();
        systemRootHostedZone = HostedZone.fromHostedZoneAttributes(this, "SystemRootZoneImport",
                existingSystemZoneAttributes);

        HostedZoneAttributes existingAppZoneAttributes = HostedZoneAttributes.builder()
                .hostedZoneId(env.getAppRootDomainZoneId())
                .zoneName(env.getAppRootDomainName())
                .build();
        appRootHostedZone = HostedZone.fromHostedZoneAttributes(this, "AppRootZoneImport",
                existingAppZoneAttributes);

        systemWildcardCertificate = DnsValidatedCertificate.Builder.create(this, "SystemDomainWildcardCertificate")
                .validation(CertificateValidation.fromDns(systemRootHostedZone))
                .domainName(IDUtils.dot("*", env.getSystemRootDomainName()))
                .hostedZone(systemRootHostedZone)
                .build();

        systemServicesWildcardCertificate = DnsValidatedCertificate.Builder.create(this, "SystemServicesWildcardCertificate")
                .validation(CertificateValidation.fromDns(systemRootHostedZone))
                .domainName(IDUtils.dot("*", CommonConfig.SERVICES_SYSTEM_SUBDOMAIN, env.getSystemRootDomainName()))
                .hostedZone(systemRootHostedZone)
                .build();

        appWildcardCertificate = DnsValidatedCertificate.Builder.create(this, "AppDomainWildcardCertificate")
                .validation(CertificateValidation.fromDns(appRootHostedZone))
                .domainName(IDUtils.dot("*", env.getAppRootDomainName()))
                .hostedZone(appRootHostedZone)
                .build();

        netSecKmsKey = Key.Builder.create(this, "NetSecGeneralKey")
                .trustAccountIdentities(true)
                .alias(NetSecConfig.KMS_ALIAS)
                .description("Used by NetSec service to encrypt user IDs and IP auth tokens")
                .build();

        SecurityGroup apiVpcLinkSecurityGroup = SecurityGroup.Builder.create(this, "ApiVpcLinkSecurityGroup")
                .vpc(serviceVpc)
                .allowAllOutbound(false)
                .build();
        apiVpcLinkSecurityGroup.addEgressRule(Peer.ipv4(serviceVpc.getVpcCidrBlock()), Port.allTraffic());
        apiVpcLink = VpcLink.Builder.create(this, "ApiVpcLink")
                .vpc(serviceVpc)
                .subnets(serviceVpc.getPublicSubnets())
                .securityGroups(List.of(apiVpcLinkSecurityGroup))
                .build();

        apiVpcNamespace = PrivateDnsNamespace.Builder.create(this, "ApiVpcNamespace")
                .vpc(serviceVpc)
                .name(IDUtils.dot("services.vpc.admiralbot.com"))
                .build();

    }

}
