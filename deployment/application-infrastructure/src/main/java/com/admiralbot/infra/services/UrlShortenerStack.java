package com.admiralbot.infra.services;

import com.admiralbot.infra.deploy.ApplicationStage;
import com.admiralbot.infra.util.ManagedPolicies;
import com.admiralbot.infra.util.Permissions;
import com.admiralbot.infra.util.Util;
import com.admiralbot.sharedconfig.CommonConfig;
import com.admiralbot.sharedconfig.UrlShortenerConfig;
import com.admiralbot.sharedutil.IDUtils;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.apigateway.*;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.BillingMode;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.lambda.Alias;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.route53.ARecord;
import software.amazon.awscdk.services.route53.RecordTarget;
import software.amazon.awscdk.services.route53.targets.ApiGateway;

import java.util.List;

public class UrlShortenerStack extends Stack {

    public UrlShortenerStack(ApplicationStage parent, String id) {
        super(parent, id);

        // Configure the DDB table to store URL information

        Attribute partitionKey = Attribute.builder()
                .name(UrlShortenerConfig.TABLE_PARTITION_KEY)
                .type(AttributeType.NUMBER)
                .build();
        Attribute sortKey = Attribute.builder()
                .name(UrlShortenerConfig.TABLE_SORT_KEY)
                .type(AttributeType.STRING)
                .build();
        Table urlTable = Table.Builder.create(this, "UrlInfoTable")
                .tableName(UrlShortenerConfig.DYNAMO_TABLE_NAME)
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .removalPolicy(RemovalPolicy.DESTROY)
                .partitionKey(partitionKey)
                .sortKey(sortKey)
                .build();

        // Configure the Lambda handler role

        Role lambdaRole = Util.standardLambdaRole(this, "HandlerFunctionRole", List.of(
                ManagedPolicies.DYNAMODB_FULL_ACCESS
        )).build();

        Permissions.addConfigPathRead(this, lambdaRole, CommonConfig.PATH);

        Alias proxyFunctionAlias = Util.highMemJavaFunction(this, "HandlerFunction", "url-shortener",
                "com.admiralbot.urlshortener.ApiGatewayLambdaHandler",
                b -> b.functionName(UrlShortenerConfig.FUNCTION_NAME).role(lambdaRole));

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


        // Configure the Lambda-backed REST API with APIGW

        EndpointConfiguration regionalEndpoint = EndpointConfiguration.builder()
                .types(List.of(EndpointType.REGIONAL))
                .build();

        LambdaRestApi restApi = LambdaRestApi.Builder.create(this, "UrlRestApi")
                .handler(proxyFunctionAlias)
                .endpointConfiguration(regionalEndpoint)
                .deployOptions(deployOptions)
                .proxy(false)
                .build();

        // Add two main resources:
        // "/admin/{proxy+}", with IAM auth (note "/admin" must also exist but has no methods)
        // "/{proxy+}", no auth, for everything else

        Resource adminResource = restApi.getRoot().addResource(UrlShortenerConfig.URL_ADMIN_PATH);
        MethodOptions iamAuthOptions = MethodOptions.builder().authorizationType(AuthorizationType.IAM).build();
        Resource adminProxyResource = adminResource.addProxy(ProxyResourceOptions.builder()
                .defaultMethodOptions(iamAuthOptions)
                .build());
        Resource fallbackRootProxyResource = restApi.getRoot().addProxy();

        // Register in system DNS zone

        restApi.addDomainName("UrlRestApiDomainName", DomainNameOptions.builder()
                .domainName(IDUtils.dot(UrlShortenerConfig.SUBDOMAIN, parent.getEnv().getSystemRootDomainName()))
                .certificate(parent.getCommonResources().getSystemWildcardCertificate())
                .endpointType(EndpointType.REGIONAL)
                .build());

        ARecord apiAliasRecord = ARecord.Builder.create(this, "UrlApiAliasRecord")
                .zone(parent.getCommonResources().getSystemRootHostedZone())
                .recordName(UrlShortenerConfig.SUBDOMAIN)
                .target(RecordTarget.fromAlias(new ApiGateway(restApi)))
                .ttl(Duration.minutes(5))
                .build();

    }

}
