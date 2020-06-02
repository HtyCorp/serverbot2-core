package io.mamish.serverbot2.infra.core;

import io.mamish.serverbot2.infra.util.Util;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedconfig.NetSecConfig;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.apigateway.*;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.route53.ARecord;
import software.amazon.awscdk.services.route53.RecordTarget;
import software.amazon.awscdk.services.route53.targets.ApiGateway;

import java.util.List;

public class IpAuthorizerStack extends Stack {

    public IpAuthorizerStack(Construct parent, String id, StackProps props, CommonStack commonStack) {
        super(parent, id, props);

        // Define function and associated role

        Role functionRole = Util.standardLambdaRole(this, "IpLambdaRole", List.of(
                Util.POLICY_STEP_FUNCTIONS_FULL_ACCESS,
                Util.POLICY_SQS_FULL_ACCESS
        )).build();

        Util.addLambdaInvokePermissionToRole(this, functionRole, NetSecConfig.FUNCTION_NAME);

        Function proxyFunction = Util.standardJavaFunction(this, "IpProxyFunction", "ip-authorizer",
                "io.mamish.serverbot2.iplambda.ApiGatewayLambdaHandler", functionRole)
                .build();

        // Domain REST API backed by function

        EndpointConfiguration regionalEndpoint = EndpointConfiguration.builder()
                .types(List.of(EndpointType.REGIONAL))
                .build();
        LambdaRestApi restApi = LambdaRestApi.Builder.create(this, "IpRestApi")
                .handler(proxyFunction)
                .endpointConfiguration(regionalEndpoint)
                .build();

        // DNS stuff: Create APIGW custom domain for this API

        restApi.addDomainName("IpRestApi", DomainNameOptions.builder()
                .domainName(NetSecConfig.AUTH_SUBDOMAIN + "." + CommonConfig.APEX_DOMAIN_NAME)
                .certificate(commonStack.getWildcardCertificate())
                .endpointType(EndpointType.REGIONAL)
                .build());

        // Assuming that adding base path mapping isn't required. Most likely taken care of by `addDomainName`.

        // Add R53 record for this custom domain.

        ARecord apiAliasRecord = ARecord.Builder.create(this, "IpApiAliasRecord")
                .zone(commonStack.getApexHostedZone())
                .recordName(NetSecConfig.AUTH_SUBDOMAIN)
                .target(RecordTarget.fromAlias(new ApiGateway(restApi)))
                .ttl(Duration.minutes(5))
                .build();

    }

}
