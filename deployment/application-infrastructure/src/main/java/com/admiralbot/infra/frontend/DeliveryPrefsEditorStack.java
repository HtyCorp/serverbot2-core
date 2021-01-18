package com.admiralbot.infra.frontend;

import com.admiralbot.infra.deploy.ApplicationGlobalStage;
import com.admiralbot.sharedconfig.CommonConfig;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.cloudfront.*;
import software.amazon.awscdk.services.cloudfront.origins.S3Origin;
import software.amazon.awscdk.services.s3.Bucket;

import java.util.List;

public class DeliveryPrefsEditorStack extends Stack {

    public DeliveryPrefsEditorStack(ApplicationGlobalStage parent, String id) {
        super(parent, id);

        Bucket contentBucket = Bucket.Builder.create(this, "ContentBucket")
                .build();
        S3Origin s3Origin = S3Origin.Builder.create(contentBucket).build();

        Distribution distribution = Distribution.Builder.create(this, "Distribution")
                .certificate(parent.getGlobalCommonStack().getSystemWildcardCertificate())
                .domainNames(List.of(CommonConfig.systemSubdomain("delivery")))
                .defaultRootObject("index.html")
                .defaultBehavior(BehaviorOptions.builder()
                        .allowedMethods(AllowedMethods.ALLOW_GET_HEAD)
                        .cachedMethods(CachedMethods.CACHE_GET_HEAD)
                        .origin(s3Origin)
                        .viewerProtocolPolicy(ViewerProtocolPolicy.REDIRECT_TO_HTTPS)
                        .build())
                .build();

    }

}
