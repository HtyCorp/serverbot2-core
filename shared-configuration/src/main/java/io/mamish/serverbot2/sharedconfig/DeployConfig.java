package io.mamish.serverbot2.sharedconfig;

/**
 * Configuration values used by CDK deployment.
 */
public class DeployConfig {

    public static final String PATH_ALL = "deployment";

    // PRIVATE

    public static final String PATH_PRIVATE = PATH_ALL + "/private";

    public static final String GITHUB_OAUTH_TOKEN_SECRET_NAME = PATH_PRIVATE+"/github-token";

    // PUBLIC

    public static final String GITHUB_DEPLOYMENT_SOURCE_OWNER = "HtyCorp";
    public static final String GITHUB_DEPLOYMENT_SOURCE_REPO = "serverbot2-core";
    public static final String GITHUB_DEPLOYMENT_SOURCE_BRANCH = "cdk-pipelines-preview";

}
