package io.mamish.serverbot2.infra.services;

import io.mamish.serverbot2.infra.deploy.ApplicationEnv;
import io.mamish.serverbot2.infra.util.Policies;
import io.mamish.serverbot2.infra.util.Util;
import io.mamish.serverbot2.sharedconfig.UrlShortenerConfig;
import io.mamish.serverbot2.sharedutil.IDUtils;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.apigateway.*;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.route53.ARecord;
import software.amazon.awscdk.services.route53.RecordTarget;
import software.amazon.awscdk.services.route53.targets.ApiGateway;

import java.util.List;

public class UrlShortenerStack extends Stack {

    public UrlShortenerStack(Construct parent, String id, CommonStack commonStack, ApplicationEnv env) {
        super(parent, id);

        // Configure the Lambda handler role

        Role lambdaRole = Util.standardLambdaRole(this, "HandlerFunctionRole", List.of(
                Policies.DYNAMODB_FULL_ACCESS
        )).build();

        Function handlerFunction = Util.standardJavaFunction(this, "HandlerFunction", "url-shortener",
                "io.mamish.serverbot2.urlshortener.ApiGatewayLambdaHandler", lambdaRole).build();

        // Configure the Lambda-backed REST API with APIGW

        EndpointConfiguration regionalEndpoint = EndpointConfiguration.builder()
                .types(List.of(EndpointType.REGIONAL))
                .build();

        LambdaRestApi restApi = LambdaRestApi.Builder.create(this, "UrlRestApi")
                .handler(handlerFunction)
                .endpointConfiguration(regionalEndpoint)
                .proxy(false)
                .build();

        // Add two main resources:
        // "/admin/{proxy+}", with IAM auth (note "/admin" must also exist but has no methods)
        // "/{proxy+}", no auth, for everything else

        Resource adminResource = restApi.getRoot().addResource(UrlShortenerConfig.URI_ADMIN_PATH);
        MethodOptions iamAuthOptions = MethodOptions.builder().authorizationType(AuthorizationType.IAM).build();
        Resource adminProxyResource = adminResource.addProxy(ProxyResourceOptions.builder()
                .defaultMethodOptions(iamAuthOptions)
                .build());
        Resource fallbackRootProxyResource = restApi.getRoot().addProxy();

        // Register in system DNS zone

        restApi.addDomainName("UrlRestApiDomainName", DomainNameOptions.builder()
                .domainName(IDUtils.dot(UrlShortenerConfig.SUBDOMAIN, env.getSystemRootDomainName()))
                .certificate(commonStack.getSystemWildcardCertificate())
                .endpointType(EndpointType.REGIONAL)
                .build());

        ARecord apiAliasRecord = ARecord.Builder.create(this, "UrlApiAliasRecord")
                .zone(commonStack.getSystemRootHostedZone())
                .recordName(UrlShortenerConfig.SUBDOMAIN)
                .target(RecordTarget.fromAlias(new ApiGateway(restApi)))
                .ttl(Duration.minutes(5))
                .build();

    }

}
