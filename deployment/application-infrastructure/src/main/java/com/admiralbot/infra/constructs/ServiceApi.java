package com.admiralbot.infra.constructs;

import com.admiralbot.framework.common.ApiAuthType;
import com.admiralbot.framework.common.ApiEndpointInfo;
import com.admiralbot.infra.deploy.ApplicationRegionalStage;
import com.admiralbot.sharedconfig.CommonConfig;
import com.admiralbot.sharedconfig.DeployConfig;
import com.admiralbot.sharedutil.Joiner;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.services.apigatewayv2.*;
import software.amazon.awscdk.services.apigatewayv2.CfnStage.AccessLogSettingsProperty;
import software.amazon.awscdk.services.apigatewayv2.integrations.HttpServiceDiscoveryIntegration;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.route53.ARecord;
import software.amazon.awscdk.services.route53.RecordTarget;
import software.amazon.awscdk.services.route53.targets.ApiGatewayv2Domain;

import java.util.List;
import java.util.Objects;

public class ServiceApi extends Construct {

    // This has to be single-line in the actual spec, so lack of newlines is deliberate.
    private static final String DEFAULT_ACCESS_LOG_FORMAT = "{ " +
            "\"requestTime\":\"$context.requestTime\", " +
            "\"requestId\":\"$context.requestId\", " +
            "\"caller\":\"$context.identity.caller\", " +
            "\"ip\": \"$context.identity.sourceIp\", " +
            "\"httpMethod\":\"$context.httpMethod\", " +
            "\"routeKey\":\"$context.routeKey\", " +
            "\"status\":\"$context.status\", " +
            "\"protocol\":\"$context.protocol\", " +
            "\"responseLength\":\"$context.responseLength\" " +
            "}";

    // Recorded so we can assert that added service interfaces all use the same service name.
    private final String serviceName;
    private final HttpApi api;
    private final VpcLink commonVpcLink;

    public ServiceApi(Construct parent, String id, ApplicationRegionalStage appStage, Class<?> mainServiceInterfaceClass) {
        super(parent, id);

        serviceName = mainServiceInterfaceClass.getAnnotation(ApiEndpointInfo.class).serviceName();
        commonVpcLink = appStage.getCommonResources().getApiVpcLink();

        // Build service name (CDK uses the given ID directly as name since they don't have to be unique)
        // Strip the leading "I" and training "Service" if present.

        String className = mainServiceInterfaceClass.getSimpleName();
        int startStrip = (className.startsWith("I") && Character.isUpperCase(className.charAt(1))) ? 1 : 0;
        int endStrip = (className.endsWith("Service")) ? "Service".length() : 0;
        String apiConstructId = className.substring(startStrip, className.length() - endStrip) + "ServiceApi";

        // CDK currently uses this ID as the API name, so to remove confusion make a compound name from the interface.
        api = HttpApi.Builder.create(this, apiConstructId)
                .createDefaultStage(false)
                .build();

        // Disable default endpoint (requires CDK escape hatch)
        CfnApi cfnApi = (CfnApi) api.getNode().getDefaultChild();
        Objects.requireNonNull(cfnApi).setDisableExecuteApiEndpoint(true);

        String fqdn = Joiner.dot(serviceName, CommonConfig.SERVICES_SYSTEM_SUBDOMAIN,
                appStage.getEnv().getSystemRootDomainName());
        DomainName serviceDomainName = DomainName.Builder.create(this, "CustomDomainName")
                .certificate(appStage.getCommonResources().getSystemServicesWildcardCertificate())
                .domainName(fqdn)
                .build();

        // Don't use default stage. Instead, name stage identically to the service name.
        // This is so that execute-api permissions can be granted based on symbolic stage names rather than API IDs,
        // since there are some nasty circular dependencies otherwise if we try to make services grant permission to
        // each other based on the generated API ID.

        HttpStage defaultStage = HttpStage.Builder.create(this, "ServiceStage")
                .httpApi(api)
                .stageName(serviceName)
                .autoDeploy(true)
                .domainMapping(DomainMappingOptions.builder()
                        .domainName(serviceDomainName)
                        .build())
                .build();

        // Configure custom DNS

        ARecord apiAliasRecord = ARecord.Builder.create(this, "DnsRecord")
                .recordName(fqdn)
                .zone(appStage.getCommonResources().getSystemRootHostedZone())
                .target(RecordTarget.fromAlias(new ApiGatewayv2Domain(serviceDomainName)))
                .ttl(Duration.seconds(CommonConfig.SERVICES_INTERNAL_DNS_TTL_SECONDS))
                .build();

        // Enable access logs (not supported in high-level constructs)

        String friendlyLogGroupName = Joiner.slash(DeployConfig.SERVICE_LOGS_PREFIX, "apiaccess", serviceName);
        LogGroup accessLogGroup = LogGroup.Builder.create(this, apiConstructId+"AccessLogs")
                .logGroupName(friendlyLogGroupName)
                .removalPolicy(RemovalPolicy.DESTROY)
                .retention(RetentionDays.THREE_MONTHS)
                .build();

        CfnStage cfnStage = (CfnStage) defaultStage.getNode().getDefaultChild();
        Objects.requireNonNull(cfnStage).setAccessLogSettings(AccessLogSettingsProperty.builder()
                .destinationArn(accessLogGroup.getLogGroupArn())
                .format(DEFAULT_ACCESS_LOG_FORMAT)
                .build());

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

        IHttpRoute route = api.addRoutes(AddRoutesOptions.builder()
                .integration(ecsServiceIntegration)
                .methods(List.of(httpMethodForEndpoint))
                .path(endpointInfo.uriPath())
                .build()
        ).get(0);

        if (endpointInfo.authType() == ApiAuthType.IAM) {
            CfnRoute cfnRoute = (CfnRoute) route.getNode().getDefaultChild();
            Objects.requireNonNull(cfnRoute).setAuthorizationType("AWS_IAM");
        }

    }

}
