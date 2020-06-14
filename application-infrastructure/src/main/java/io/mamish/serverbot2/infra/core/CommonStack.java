package io.mamish.serverbot2.infra.core;

import io.mamish.serverbot2.infra.util.Util;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedconfig.NetSecConfig;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.certificatemanager.DnsValidatedCertificate;
import software.amazon.awscdk.services.certificatemanager.ValidationMethod;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.kms.Key;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.route53.HostedZone;
import software.amazon.awscdk.services.route53.HostedZoneProviderProps;
import software.amazon.awscdk.services.route53.IHostedZone;

import java.util.List;
import java.util.Map;

public class CommonStack extends Stack {

    private final Vpc serviceVpc;
    private final Vpc applicationVpc;
    private final IHostedZone apexHostedZone;
    private final DnsValidatedCertificate wildcardCertificate;
    private final Key netSecKmsKey;

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

    public CommonStack(Construct parent, String id, StackProps props) {
        super(parent, id, props);

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
                .trafficType(FlowLogTrafficType.ALL)
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

        HostedZoneProviderProps existingZoneLookup = HostedZoneProviderProps.builder()
                .domainName(CommonConfig.APEX_DOMAIN_NAME)
                .build();
        apexHostedZone = HostedZone.fromLookup(this, "ApexHostedZone", existingZoneLookup);

        Util.instantiateConfigSsmParameter(this, "HostedZoneIdParameter",
                CommonConfig.HOSTED_ZONE_ID, apexHostedZone.getHostedZoneId());

        wildcardCertificate = DnsValidatedCertificate.Builder.create(this, "DomainWildcardCertificate")
                .validationMethod(ValidationMethod.DNS)
                .domainName("*."+CommonConfig.APEX_DOMAIN_NAME)
                .hostedZone(apexHostedZone)
                .build();

        netSecKmsKey = Key.Builder.create(this, "NetSecGeneralKey")
                .trustAccountIdentities(true)
                .alias(NetSecConfig.KMS_ALIAS)
                .build();

    }

}
