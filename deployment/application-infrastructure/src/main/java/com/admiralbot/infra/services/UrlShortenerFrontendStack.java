package com.admiralbot.infra.services;

import com.admiralbot.infra.deploy.ApplicationGlobalStage;
import com.admiralbot.infra.util.Permissions;
import com.admiralbot.infra.util.Util;
import com.admiralbot.urlshortener.model.IUrlShortener;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.cloudfront.*;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;

import java.util.List;

public class UrlShortenerFrontendStack extends Stack {

    public UrlShortenerFrontendStack(ApplicationGlobalStage parent, String id) {
        super(parent, id);

        Function edgeFunction = Function.Builder.create(this, "EdgeFunction")
                .runtime(Runtime.PYTHON_3_8)
                .code(Code.fromAsset(Util.codeBuildPath("web", "url-shortener-frontend", "edge-function").toString()))
                .memorySize(512)
                .build();
        Permissions.addExecuteApi(this, edgeFunction, IUrlShortener.class);

        EdgeLambda functionAssociation = EdgeLambda.builder()
                .eventType(LambdaEdgeEventType.VIEWER_REQUEST)
                .functionVersion(edgeFunction.getCurrentVersion())
                .includeBody(false)
                .build();

        BehaviorOptions defaultBehavior = BehaviorOptions.builder()
                .edgeLambdas(List.of(functionAssociation))
                //.("/*/*")
                // TODO
                .build();

        Distribution cfDistro = Distribution.Builder.create(this, "MainDistribution")
                .certificate(parent.getGlobalCommonStack().getSystemWildcardCertificate())
                .defaultBehavior(defaultBehavior)
                // TODO
                .build();

    }
}
