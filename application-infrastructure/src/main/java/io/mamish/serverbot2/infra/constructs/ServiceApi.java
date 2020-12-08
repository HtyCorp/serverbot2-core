package io.mamish.serverbot2.infra.constructs;

import io.mamish.serverbot2.framework.common.ApiEndpointInfo;
import io.mamish.serverbot2.infra.deploy.ApplicationStage;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedutil.IDUtils;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.services.apigatewayv2.*;
import software.amazon.awscdk.services.apigatewayv2.integrations.HttpServiceDiscoveryIntegration;

import java.util.List;

public class ServiceApi extends Construct {

    // Recorded so we can assert that added service interfaces all use the same service name.
    private final String serviceName;
    private final HttpApi api;
    private final VpcLink commonVpcLink;

    public ServiceApi(Construct parent, String id, ApplicationStage appStage, Class<?> mainServiceInterfaceClass) {
        super(parent, id);

        serviceName = mainServiceInterfaceClass.getAnnotation(ApiEndpointInfo.class).serviceName();
        commonVpcLink = appStage.getCommonResources().getApiVpcLink();

        api = HttpApi.Builder.create(this, "ServiceApi")
                .createDefaultStage(false)
                .build();

        String fqdn = IDUtils.dot(serviceName, CommonConfig.SERVICES_SYSTEM_SUBDOMAIN,
                appStage.getEnv().getSystemRootDomainName());
        DomainName serviceDomainName = DomainName.Builder.create(this, "CustomDomainName")
                .certificate(appStage.getCommonResources().getSystemServicesWildcardCertificate())
                .domainName(fqdn)
                .build();

        // Don't use default stage. Instead, name stage identically to the service name.
        // This is so that execute-api permissions can be granted based on symbolic stage names rather than API IDs,
        // since there are some nasty circular dependencies otherwise if we try to make services grant permission to
        // each other based on the generated API ID.

        HttpStage.Builder.create(this, "ServiceStage")
                .httpApi(api)
                .stageName(serviceName)
                .autoDeploy(true)
                .domainMapping(DomainMappingOptions.builder()
                        .domainName(serviceDomainName)
                        .build())
                .build();
    }

    public void addEcsRoute(Class<?> serviceInterfaceClass, EcsMicroservice ecsMicroservice) {
        ApiEndpointInfo endpointInfo = serviceInterfaceClass.getAnnotation(ApiEndpointInfo.class);

        if (!endpointInfo.serviceName().equals(serviceName)) {
            throw new IllegalArgumentException("Requested service name '"+endpointInfo.serviceName()+"' doesn't match " +
                    "set service name '"+serviceName+"'.");
        }

        HttpMethod httpMethodForEndpoint = HttpMethod.valueOf(endpointInfo.httpMethod().name());

        HttpServiceDiscoveryIntegration ecsServiceIntegration = HttpServiceDiscoveryIntegration.Builder.create()
                .service(ecsMicroservice.getInternalDiscoveryService())
                .method(httpMethodForEndpoint)
                .vpcLink(commonVpcLink)
                .build();

        api.addRoutes(AddRoutesOptions.builder()
                .integration(ecsServiceIntegration)
                .methods(List.of(httpMethodForEndpoint))
                .path(endpointInfo.uriPath())
                .build());
    }

}
