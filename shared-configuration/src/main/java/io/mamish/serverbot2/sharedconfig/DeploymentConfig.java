package io.mamish.serverbot2.sharedconfig;

public class DeploymentConfig {

    public static final String ARTIFACT_BUCKET_NAME = "htycorp-serverbot2-test-codepipeline-artifact-bucket";

    public static final String GITHUB_OAUTH_TOKEN_SECRET_NAME = "deployment/github-token";

    public static final String GITHUB_DEPLOYMENT_SOURCE_OWNER = "MamishIo";
    public static final String GITHUB_DEPLOYMENT_SOURCE_REPO = "serverbot2-core";
    public static final String GITHUB_DEPLOYMENT_SOURCE_BRANCH = "test-release";
}
