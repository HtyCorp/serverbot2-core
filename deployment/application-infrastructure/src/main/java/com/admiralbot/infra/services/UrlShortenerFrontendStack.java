package com.admiralbot.infra.services;

import com.admiralbot.framework.common.ApiEndpointInfo;
import com.admiralbot.infra.deploy.ApplicationGlobalStage;
import com.admiralbot.infra.util.Permissions;
import com.admiralbot.infra.util.Util;
import com.admiralbot.sharedconfig.CommonConfig;
import com.admiralbot.sharedconfig.UrlShortenerConfig;
import com.admiralbot.sharedutil.Joiner;
import com.admiralbot.urlshortener.model.IUrlShortener;
import com.google.gson.Gson;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.cloudfront.*;
import software.amazon.awscdk.services.cloudfront.origins.S3Origin;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.route53.ARecord;
import software.amazon.awscdk.services.route53.RecordTarget;
import software.amazon.awscdk.services.route53.targets.CloudFrontTarget;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awssdk.core.SdkBytes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

public class UrlShortenerFrontendStack extends Stack {

    private final Gson gson = new Gson();

    public UrlShortenerFrontendStack(ApplicationGlobalStage parent, String id) {
        super(parent, id);

        // Basic content bucket: for now this has no actual content (everything is generated by edge Lambda)

        Bucket basicContentBucket = Bucket.Builder.create(this, "BasicContentBucket")
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        // Edge function

        // Build target metadata for URL shortener service and supply it to edge function env vars
        ApiEndpointInfo urlShortenerEndpointInfo = IUrlShortener.class.getAnnotation(ApiEndpointInfo.class);
        String urlShortenerServiceHost = String.format("%s.%s.%s",
                urlShortenerEndpointInfo.serviceName(),
                CommonConfig.SERVICES_SYSTEM_SUBDOMAIN,
                parent.getMainEnv().getSystemRootDomainName()
        );
        String urlShortenerServiceUrl = String.format("https://%s%s",
                urlShortenerServiceHost,
                urlShortenerEndpointInfo.uriPath()
        );

        // Edge functions don't support standard env vars, so we have to supply it by proxy as a file
        Map<String,String> functionEnvironmentVariables = Map.of(
                "SB2_TARGET_REGION", parent.getMainEnv().getRegion(),
                "SB2_TARGET_HOST", urlShortenerServiceHost,
                "SB2_TARGET_URL", urlShortenerServiceUrl
        );
        String functionEnvironmentVariablesJson = gson.toJson(functionEnvironmentVariables);
        InputStream envVarsFileInput = SdkBytes.fromUtf8String(gson.toJson(functionEnvironmentVariablesJson)).asInputStream();
        Path codePath = Util.codeBuildPath("web", "url-shortener-frontend", "edge-function");
        Path envVarsFileDest = codePath.resolve("environment.json");
        try {
            Files.copy(envVarsFileInput, envVarsFileDest, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't copy edge function env vars due to IO error", e);
        }

        // Declare function
        Function edgeFunction = Function.Builder.create(this, "EdgeFunction")
                .runtime(Runtime.PYTHON_3_8)
                .code(Code.fromAsset(codePath.toString()))
                .handler("edge_function.lambda_handler")
                .memorySize(512)
                .build();
        Permissions.addExecuteApi(this, edgeFunction, IUrlShortener.class);

        // CloudFront distribution

        IOrigin s3Origin = S3Origin.Builder.create(basicContentBucket)
                .build();

        EdgeLambda edgeLambdaAssociation = EdgeLambda.builder()
                .eventType(LambdaEdgeEventType.ORIGIN_REQUEST)
                .functionVersion(edgeFunction.getCurrentVersion())
                .includeBody(false)
                .build();

        BehaviorOptions defaultBehavior = BehaviorOptions.builder()
                .edgeLambdas(List.of(edgeLambdaAssociation))
                .allowedMethods(AllowedMethods.ALLOW_GET_HEAD)
                .cachedMethods(CachedMethods.CACHE_GET_HEAD)
                .viewerProtocolPolicy(ViewerProtocolPolicy.REDIRECT_TO_HTTPS)
                .origin(s3Origin)
                .build();

        String fqdn = Joiner.dot(UrlShortenerConfig.SUBDOMAIN, parent.getMainEnv().getSystemRootDomainName());
        Distribution cfDistro = Distribution.Builder.create(this, "MainDistribution")
                .certificate(parent.getGlobalCommonStack().getSystemWildcardCertificate())
                .defaultBehavior(defaultBehavior)
                .domainNames(List.of(fqdn))
                .build();

        ARecord cfAliasRecord = ARecord.Builder.create(this, "CloudFrontDnsRecord")
                .zone(parent.getGlobalCommonStack().getSystemRootHostedZone())
                .recordName(UrlShortenerConfig.SUBDOMAIN)
                .ttl(Duration.minutes(60))
                .target(RecordTarget.fromAlias(new CloudFrontTarget(cfDistro)))
                .build();

    }
}
