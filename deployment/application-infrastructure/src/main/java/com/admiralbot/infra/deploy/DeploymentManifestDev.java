package com.admiralbot.infra.deploy;

public class DeploymentManifestDev {

    private String repoSourceBranch;

    private ApplicationEnv environment;

    public String getRepoSourceBranch() {
        return repoSourceBranch;
    }

    public ApplicationEnv getEnvironment() {
        return environment;
    }

}
