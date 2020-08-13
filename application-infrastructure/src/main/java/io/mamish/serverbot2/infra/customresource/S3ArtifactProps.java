package io.mamish.serverbot2.infra.customresource;

import software.amazon.awscdk.services.s3.IBucket;
import software.amazon.awscdk.services.s3.assets.Asset;

public class S3ArtifactProps {

    private Asset sourceS3Asset;
    private IBucket targetBucket;
    private String artifactKeyPrefix;
    private String artifactKeySuffix;
    private String artifactS3UrlParameterName;

    public S3ArtifactProps(Asset sourceS3Asset, IBucket targetBucket, String artifactKeyPrefix,
                           String artifactKeySuffix, String artifactS3UrlParameterName) {
        this.sourceS3Asset = sourceS3Asset;
        this.targetBucket = targetBucket;
        this.artifactKeyPrefix = artifactKeyPrefix;
        this.artifactKeySuffix = artifactKeySuffix;
        this.artifactS3UrlParameterName = artifactS3UrlParameterName;
    }

    public Asset getSourceS3Asset() {
        return sourceS3Asset;
    }

    public IBucket getTargetBucket() {
        return targetBucket;
    }

    public String getArtifactKeyPrefix() {
        return artifactKeyPrefix;
    }

    public String getArtifactKeySuffix() {
        return artifactKeySuffix;
    }

    public String getArtifactS3UrlParameterName() {
        return artifactS3UrlParameterName;
    }
}
