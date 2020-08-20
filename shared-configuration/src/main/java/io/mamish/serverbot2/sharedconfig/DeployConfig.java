package io.mamish.serverbot2.sharedconfig;

/**
 * Configuration values used by CDK deployment.
 */
public class DeployConfig {

    public static final String ENVIRONMENT_MANIFEST_PARAM_NAME = "DeploymentEnvironmentManifest";
    public static final String GITHUB_OAUTH_TOKEN_SECRET_NAME = "GitHubRepoAccessToken";
    public static final String GITHUB_DEPLOYMENT_SOURCE_OWNER = "HtyCorp";
    public static final String GITHUB_DEPLOYMENT_SOURCE_REPO = "serverbot2-core";
    public static final String GITHUB_DEPLOYMENT_SOURCE_BRANCH = "master";

}
