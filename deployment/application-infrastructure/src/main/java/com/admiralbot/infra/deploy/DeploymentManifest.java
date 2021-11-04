package com.admiralbot.infra.deploy;

import java.util.List;

public class DeploymentManifest {

    private String connectionUuid;
    private List<ApplicationEnv> environments;

    public String getConnectionUuid() {
        return connectionUuid;
    }

    public List<ApplicationEnv> getEnvironments() {
        return environments;
    }

}
