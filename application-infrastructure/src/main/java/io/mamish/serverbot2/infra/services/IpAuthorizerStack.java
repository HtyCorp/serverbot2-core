package io.mamish.serverbot2.infra.services;

import io.mamish.serverbot2.infra.deploy.ApplicationEnv;
import io.mamish.serverbot2.infra.util.Policies;
import io.mamish.serverbot2.infra.util.Util;
import io.mamish.serverbot2.sharedconfig.NetSecConfig;
import io.mamish.serverbot2.sharedutil.IDUtils;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.apigateway.DomainNameOptions;
import software.amazon.awscdk.services.apigateway.EndpointConfiguration;
import software.amazon.awscdk.services.apigateway.EndpointType;
import software.amazon.awscdk.services.apigateway.LambdaRestApi;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.lambda.Alias;
import software.amazon.awscdk.services.route53.ARecord;
import software.amazon.awscdk.services.route53.RecordTarget;
import software.amazon.awscdk.services.route53.targets.ApiGateway;

import java.util.List;

public class IpAuthorizerStack extends Stack {

    public IpAuthorizerStack(Construct parent, String id, CommonStack commonStack, ApplicationEnv env) {
        super(parent, id);

        // Define function and associated role

        Role functionRole = Util.standardLambdaRole(this, "IpLambdaRole", List.of(
                Policies.STEP_FUNCTIONS_FULL_ACCESS,
                Policies.SQS_FULL_ACCESS
        )).build();

        Util.addLambdaInvokePermissionToRole(this, functionRole, NetSecConfig.FUNCTION_NAME);

        Alias proxyFunctionAlias = Util.provisionedJavaFunction(this, "IpProxyFunction", "ip-authorizer",
                "io.mamish.serverbot2.iplambda.ApiGatewayLambdaHandler", 1,
                b-> b.role(functionRole));

        // Domain REST API backed by function

        EndpointConfiguration regionalEndpoint = EndpointConfiguration.builder()
                .types(List.of(EndpointType.REGIONAL))
                .build();
        LambdaRestApi restApi = LambdaRestApi.Builder.create(this, "IpRestApi")
                .handler(proxyFunctionAlias)
                .endpointConfiguration(regionalEndpoint)
                .build();

        // DNS stuff: Create APIGW custom domain for this API

        restApi.addDomainName("IpRestApi", DomainNameOptions.builder()
                .domainName(IDUtils.dot(NetSecConfig.AUTH_SUBDOMAIN, env.getSystemRootDomainName()))
                .certificate(commonStack.getSystemWildcardCertificate())
                .endpointType(EndpointType.REGIONAL)
                .build());

        // Assuming that adding base path mapping isn't required. Most likely taken care of by `addDomainName`.

        // Add R53 record for this custom domain.

        ARecord apiAliasRecord = ARecord.Builder.create(this, "IpApiAliasRecord")
                .zone(commonStack.getSystemRootHostedZone())
                .recordName(NetSecConfig.AUTH_SUBDOMAIN)
                .target(RecordTarget.fromAlias(new ApiGateway(restApi)))
                .ttl(Duration.minutes(5))
                .build();

    }

}
