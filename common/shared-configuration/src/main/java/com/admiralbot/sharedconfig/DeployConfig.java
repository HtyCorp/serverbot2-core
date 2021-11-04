package com.admiralbot.sharedconfig;

/**
 * Configuration values used by CDK deployment.
 */
public class DeployConfig {

    public static final String DEPLOYMENT_MANIFEST_PARAM_NAME = "DeploymentEnvironmentManifest";
    public static final String GITHUB_DEPLOYMENT_SOURCE_OWNER = "HtyCorp";
    public static final String GITHUB_DEPLOYMENT_SOURCE_REPO = "serverbot2-core";
    public static final String GITHUB_DEPLOYMENT_MASTER_BRANCH = "master";

    // Simple-item form of the deploy env manifest for local development
    public static final String DEV_ENVIRONMENT_PARAM_NAME = "DevelopmentEnvironment";

    public static final String SERVICE_LOGS_PREFIX = "serverbot2/svc";

}
