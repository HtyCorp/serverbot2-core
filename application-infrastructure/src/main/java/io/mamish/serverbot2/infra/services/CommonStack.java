package io.mamish.serverbot2.infra.services;

import io.mamish.serverbot2.infra.deploy.ApplicationEnv;
import io.mamish.serverbot2.infra.util.Util;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedconfig.DiscordConfig;
import io.mamish.serverbot2.sharedconfig.NetSecConfig;
import io.mamish.serverbot2.sharedutil.IDUtils;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.core.SecretValue;
import software.amazon.awscdk.core.Stack;
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

import java.util.List;
import java.util.Map;

public class CommonStack extends Stack {

    private final Bucket deployedArtifactBucket;
    private final Vpc serviceVpc;
    private final Vpc applicationVpc;
    private final IHostedZone apexHostedZone;
    private final DnsValidatedCertificate wildcardCertificate;
    private final Key netSecKmsKey;

    public Bucket getDeployedArtifactBucket() {
        return deployedArtifactBucket;
    }

    public Vpc getServiceVpc() {
        return serviceVpc;
    }

    public Vpc getApplicationVpc() {
        return applicationVpc;
    }

    public IHostedZone getApexHostedZone() {
        return apexHostedZone;
    }

    public DnsValidatedCertificate getWildcardCertificate() {
        return wildcardCertificate;
    }

    public Key getNetSecKmsKey() {
        return netSecKmsKey;
    }

    public CommonStack(Construct parent, String id, ApplicationEnv env) {
        super(parent, id);

        SecretValue discordApiTokenSource = SecretValue.secretsManager(env.getDiscordApiTokenSourceSecretArn());
        Util.instantiateConfigSecret(this, "DiscordApiTokenSecret",
                DiscordConfig.API_TOKEN, discordApiTokenSource.toString());

        Util.instantiateConfigSsmParameter(this, "HostedZoneIdParam",
                CommonConfig.HOSTED_ZONE_ID, env.getRoute53ZoneId());
        Util.instantiateConfigSsmParameter(this, "DomainNameParam",
                CommonConfig.ROOT_DOMAIN_NAME, env.getDomainName());
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

        appFlowLogsGroup.getNode().findChild("Resource");

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

        HostedZoneAttributes existingZoneAttributes = HostedZoneAttributes.builder()
                .hostedZoneId(env.getRoute53ZoneId())
                .zoneName(env.getDomainName())
                .build();
        apexHostedZone = HostedZone.fromHostedZoneAttributes(this, "ApexHostedZoneImport",
                existingZoneAttributes);

        wildcardCertificate = DnsValidatedCertificate.Builder.create(this, "DomainWildcardCertificate")
                .validation(CertificateValidation.fromDns(apexHostedZone))
                .domainName(IDUtils.dot("*", env.getDomainName()))
                .hostedZone(apexHostedZone)
                .build();

        netSecKmsKey = Key.Builder.create(this, "NetSecGeneralKey")
                .trustAccountIdentities(true)
                .alias(NetSecConfig.KMS_ALIAS)
                .build();

    }

}
