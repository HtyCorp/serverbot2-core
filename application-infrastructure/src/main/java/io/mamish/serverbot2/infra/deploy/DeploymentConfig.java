package io.mamish.serverbot2.infra.deploy;

import java.util.List;

public class DeploymentConfig {

    private List<DeploymentEnv> environments;

    public List<DeploymentEnv> getEnvironments() {
        return environments;
    }
}
