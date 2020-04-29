package io.mamish.serverbot2.appinfra;

import io.mamish.serverbot2.sharedconfig.CommonConfig;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.certificatemanager.DnsValidatedCertificate;
import software.amazon.awscdk.services.certificatemanager.ValidationMethod;
import software.amazon.awscdk.services.route53.HostedZone;
import software.amazon.awscdk.services.route53.HostedZoneProviderProps;
import software.amazon.awscdk.services.route53.IHostedZone;

public class CommonStack extends Stack {

    public CommonStack(Construct parent, String id) {
        this(parent, id, null);
    }

    private IHostedZone apexHostedZone;
    private DnsValidatedCertificate wildcardCertificate;

    public IHostedZone getApexHostedZone() {
        return apexHostedZone;
    }

    public DnsValidatedCertificate getWildcardCertificate() {
        return wildcardCertificate;
    }

    public CommonStack(Construct parent, String id, StackProps props) {
        super(parent, id, props);

        HostedZoneProviderProps existingZoneLookup = HostedZoneProviderProps.builder()
                .domainName(CommonConfig.APEX_DOMAIN_NAME)
                .build();
        apexHostedZone = HostedZone.fromLookup(this, "ApexHostedZone", existingZoneLookup);

        wildcardCertificate = DnsValidatedCertificate.Builder.create(this, "DomainWildcardCertificate")
                .validationMethod(ValidationMethod.DNS)
                .region(CommonConfig.REGION_NAME)
                .domainName("*."+CommonConfig.APEX_DOMAIN_NAME)
                .hostedZone(apexHostedZone)
                .build();

    }

}
