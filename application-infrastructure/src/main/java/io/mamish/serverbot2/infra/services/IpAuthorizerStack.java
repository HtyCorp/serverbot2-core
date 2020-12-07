package io.mamish.serverbot2.infra.services;

import io.mamish.serverbot2.infra.deploy.ApplicationStage;
import io.mamish.serverbot2.infra.util.ManagedPolicies;
import io.mamish.serverbot2.infra.util.Util;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedconfig.IpAuthConfig;
import io.mamish.serverbot2.sharedconfig.NetSecConfig;
import io.mamish.serverbot2.sharedutil.IDUtils;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.apigateway.*;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.lambda.Alias;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.route53.ARecord;
import software.amazon.awscdk.services.route53.RecordTarget;
import software.amazon.awscdk.services.route53.targets.ApiGatewayDomain;

import java.util.List;
import java.util.Map;

public class IpAuthorizerStack extends Stack {

    private final static Map<String,String> SUBDOMAINS = Map.of(
            "primary", NetSecConfig.AUTHORIZER_SUBDOMAIN,
            "legacy", NetSecConfig.AUTHORIZER_SUBDOMAIN_LEGACY
    );

    public IpAuthorizerStack(ApplicationStage parent, String id) {
        super(parent, id);

        // Define function and associated role

        Role functionRole = Util.standardLambdaRole(this, "IpLambdaRole", List.of(
                ManagedPolicies.STEP_FUNCTIONS_FULL_ACCESS,
                ManagedPolicies.SQS_FULL_ACCESS
        )).build();

        Util.addConfigPathReadPermissionToRole(this, functionRole, CommonConfig.PATH);
        Util.addLambdaInvokePermissionToRole(this, functionRole, NetSecConfig.FUNCTION_NAME);

        Alias proxyFunctionAlias = Util.highMemJavaFunction(this, "IpProxyFunction", "ip-authorizer",
                "io.mamish.serverbot2.iplambda.ApiGatewayLambdaHandler",
                b-> b.functionName(IpAuthConfig.FUNCTION_NAME).role(functionRole));

        // Logging for API deployment

        LogGroup accessLogs = LogGroup.Builder.create(this, "ApiAccessLogs")
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();
        LogGroupLogDestination logDestination = new LogGroupLogDestination(accessLogs);
        StageOptions deployOptions = StageOptions.builder()
                .accessLogDestination(logDestination)
                .tracingEnabled(true)
                .metricsEnabled(true)
                .dataTraceEnabled(true)
                .build();

        // Domain REST API backed by function

        EndpointConfiguration regionalEndpoint = EndpointConfiguration.builder()
                .types(List.of(EndpointType.REGIONAL))
                .build();
        LambdaRestApi restApi = LambdaRestApi.Builder.create(this, "IpRestApi")
                .handler(proxyFunctionAlias)
                .endpointConfiguration(regionalEndpoint)
                .deployOptions(deployOptions)
                .build();

        // DNS stuff: Create APIGW custom domain for this API

        SUBDOMAINS.forEach((name, subdomain) -> {

            DomainName domain = restApi.addDomainName(name+"RestApi", DomainNameOptions.builder()
                    .domainName(IDUtils.dot(subdomain, parent.getEnv().getSystemRootDomainName()))
                    .certificate(parent.getCommonResources().getSystemWildcardCertificate())
                    .endpointType(EndpointType.REGIONAL)
                    .build());

            ApiGatewayDomain route53AliasTarget = new ApiGatewayDomain(domain);

            ARecord record = ARecord.Builder.create(this, name+"Record")
                    .zone(parent.getCommonResources().getSystemRootHostedZone())
                    .recordName(subdomain)
                    .target(RecordTarget.fromAlias(route53AliasTarget))
                    .ttl(Duration.minutes(5))
                    .build();

        });

    }

}
