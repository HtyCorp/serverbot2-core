package io.mamish.serverbot2.appinfra;

import io.mamish.serverbot2.sharedconfig.CommonConfig;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.certificatemanager.DnsValidatedCertificate;
import software.amazon.awscdk.services.certificatemanager.ValidationMethod;
import software.amazon.awscdk.services.ec2.SubnetConfiguration;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.route53.HostedZone;
import software.amazon.awscdk.services.route53.HostedZoneProviderProps;
import software.amazon.awscdk.services.route53.IHostedZone;
import software.amazon.awscdk.services.ssm.StringParameter;

import java.util.List;

public class CommonStack extends Stack {

    public CommonStack(Construct parent, String id) {
        this(parent, id, null);
    }

    private final Vpc applicationVpc;
    private final IHostedZone apexHostedZone;
    private final DnsValidatedCertificate wildcardCertificate;

    public Vpc getApplicationVpc() {
        return applicationVpc;
    }

    public IHostedZone getApexHostedZone() {
        return apexHostedZone;
    }

    public DnsValidatedCertificate getWildcardCertificate() {
        return wildcardCertificate;
    }

    public CommonStack(Construct parent, String id, StackProps props) {
        super(parent, id, props);

        List<SubnetConfiguration> singlePublicSubnet = List.of(SubnetConfiguration.builder()
                .name("main")
                .subnetType(SubnetType.PUBLIC)
                .cidrMask(24)
                .build());

        applicationVpc = Vpc.Builder.create(this, "ApplicationVpc")
                .cidr(CommonConfig.APPLICATION_VPC_CIDR)
                //.flowLogs() // TODO: for idle connection tracker
                .maxAzs(3)
                .subnetConfiguration(singlePublicSubnet)
                .build();

        StringParameter vpcIdParameter = Util.instantiateConfigSsmParameter(this, "VpcIdParameter",
                        CommonConfig.APPLICATION_VPC_ID, applicationVpc.getVpcId()).build();

        HostedZoneProviderProps existingZoneLookup = HostedZoneProviderProps.builder()
                .domainName(CommonConfig.APEX_DOMAIN_NAME)
                .build();
        apexHostedZone = HostedZone.fromLookup(this, "ApexHostedZone", existingZoneLookup);

        wildcardCertificate = DnsValidatedCertificate.Builder.create(this, "DomainWildcardCertificate")
                .validationMethod(ValidationMethod.DNS)
                .domainName("*."+CommonConfig.APEX_DOMAIN_NAME)
                .hostedZone(apexHostedZone)
                .build();

    }

}
