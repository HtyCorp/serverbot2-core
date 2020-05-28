package io.mamish.serverbot2.infra.core;

import io.mamish.serverbot2.infra.util.Util;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedconfig.NetSecConfig;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.apigateway.DomainNameOptions;
import software.amazon.awscdk.services.apigateway.EndpointType;
import software.amazon.awscdk.services.apigateway.LambdaRestApi;
import software.amazon.awscdk.services.iam.IManagedPolicy;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.route53.ARecord;
import software.amazon.awscdk.services.route53.RecordTarget;
import software.amazon.awscdk.services.route53.targets.ApiGateway;

import java.util.List;

public class IpStack extends Stack {

    public IpStack(Construct parent, String id, CommonStack commonStack) {
        this(parent, id, null, commonStack);
    }

    public IpStack(Construct parent, String id, StackProps props, CommonStack commonStack) {
        super(parent, id, props);

        // Define function and associated role

        List<IManagedPolicy> policyList = List.of(
                ManagedPolicy.fromAwsManagedPolicyName("service-role/AWSLambdaBasicExecutionRole"),
                ManagedPolicy.fromAwsManagedPolicyName("AWSStepFunctionsFullAccess"),
                ManagedPolicy.fromAwsManagedPolicyName("AmazonSQSFullAccess")
        );
        Role functionRole = Role.Builder.create(this, "CommandFunctionExecutionRole")
                .assumedBy(new ServicePrincipal("lambda.amazonaws.com"))
                .managedPolicies(policyList)
                .build();

        Function proxyFunction = Function.Builder.create(this, "IpProxyFunction")
                .runtime(Runtime.JAVA_11)
                .code(Util.mavenJarAsset("ip-lambda"))
                .handler("io.mamish.serverbot2.iplambda.ApiGatewayLambdaHandler")
                .role(functionRole)
                .build();

        // Domain REST API backed by function

        LambdaRestApi restApi = LambdaRestApi.Builder.create(this, "IpRestApi")
                .handler(proxyFunction)
                .build();

        // DNS stuff: Create APIGW custom domain for this API

        restApi.addDomainName("IpRestApi", DomainNameOptions.builder()
                .domainName(NetSecConfig.AUTH_SUBDOMAIN + CommonConfig.APEX_DOMAIN_NAME)
                .certificate(commonStack.getWildcardCertificate())
                .endpointType(EndpointType.REGIONAL)
                .build());

        // Assuming that adding base path mapping isn't required. Most likely taken care of by `addDomainName`.

        // Add R53 record for this custom domain.

        ARecord apiAliasRecord = ARecord.Builder.create(this, "IpApiAliasRecord")
                .zone(commonStack.getApexHostedZone())
                .recordName("ip")
                .target(RecordTarget.fromAlias(new ApiGateway(restApi)))
                .ttl(Duration.minutes(5))
                .build();

    }

}
