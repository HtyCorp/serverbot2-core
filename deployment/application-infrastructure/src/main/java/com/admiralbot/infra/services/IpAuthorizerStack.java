package com.admiralbot.infra.services;

import com.admiralbot.infra.deploy.ApplicationStage;
import com.admiralbot.infra.util.ManagedPolicies;
import com.admiralbot.infra.util.Permissions;
import com.admiralbot.infra.util.Util;
import com.admiralbot.networksecurity.model.INetworkSecurity;
import com.admiralbot.sharedconfig.CommonConfig;
import com.admiralbot.sharedconfig.IpAuthConfig;
import com.admiralbot.sharedconfig.NetSecConfig;
import com.admiralbot.sharedutil.IDUtils;
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

    // This is left over from a subdomain migration where a second 'legacy' domain was also required
    private final static Map<String,String> SUBDOMAINS = Map.of(
            "primary", NetSecConfig.AUTHORIZER_SUBDOMAIN
    );

    public IpAuthorizerStack(ApplicationStage parent, String id) {
        super(parent, id);

        // Define function and associated role

        Role functionRole = Util.standardLambdaRole(this, "IpLambdaRole", List.of(
                ManagedPolicies.STEP_FUNCTIONS_FULL_ACCESS,
                ManagedPolicies.SQS_FULL_ACCESS
        )).build();

        Permissions.addConfigPathRead(this, functionRole, CommonConfig.PATH);
        Permissions.addExecuteApi(this, functionRole, INetworkSecurity.class);

        Alias proxyFunctionAlias = Util.highMemJavaFunction(this, "IpProxyFunction", "ip-authorizer",
                "com.admiralbot.iplambda.ApiGatewayLambdaHandler",
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
