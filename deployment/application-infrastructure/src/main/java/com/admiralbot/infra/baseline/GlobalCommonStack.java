package com.admiralbot.infra.baseline;

import com.admiralbot.infra.deploy.ApplicationEnv;
import com.admiralbot.infra.deploy.ApplicationGlobalStage;
import com.admiralbot.sharedutil.Joiner;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.certificatemanager.CertificateValidation;
import software.amazon.awscdk.services.certificatemanager.DnsValidatedCertificate;
import software.amazon.awscdk.services.route53.HostedZone;
import software.amazon.awscdk.services.route53.HostedZoneAttributes;
import software.amazon.awscdk.services.route53.IHostedZone;

public class GlobalCommonStack extends Stack {

    private final IHostedZone systemRootHostedZone;
    private final DnsValidatedCertificate systemWildcardCertificate;

    public IHostedZone getSystemRootHostedZone() {
        return systemRootHostedZone;
    }

    public DnsValidatedCertificate getSystemWildcardCertificate() {
        return systemWildcardCertificate;
    }

    public GlobalCommonStack(ApplicationGlobalStage parent, String id) {
        super(parent, id);
        ApplicationEnv env = parent.getMainEnv();

        HostedZoneAttributes existingSystemZoneAttributes = HostedZoneAttributes.builder()
                .hostedZoneId(env.getSystemRootDomainZoneId())
                .zoneName(env.getSystemRootDomainName())
                .build();
        systemRootHostedZone = HostedZone.fromHostedZoneAttributes(this, "SystemRootZoneImport",
                existingSystemZoneAttributes);

        systemWildcardCertificate = DnsValidatedCertificate.Builder.create(this, "SystemDomainWildcardCertificate")
                .validation(CertificateValidation.fromDns(systemRootHostedZone))
                .domainName(Joiner.dot("*", env.getSystemRootDomainName()))
                .hostedZone(systemRootHostedZone)
                .build();

    }

}
