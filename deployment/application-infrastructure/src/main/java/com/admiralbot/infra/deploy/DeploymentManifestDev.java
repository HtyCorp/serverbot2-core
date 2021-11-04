package com.admiralbot.infra.deploy;

public class DeploymentManifestDev {

    private String connectionUuid;
    private String repoSourceBranch;
    private ApplicationEnv environment;

    public String getConnectionUuid() {
        return connectionUuid;
    }

    public String getRepoSourceBranch() {
        return repoSourceBranch;
    }

    public ApplicationEnv getEnvironment() {
        return environment;
    }

}
