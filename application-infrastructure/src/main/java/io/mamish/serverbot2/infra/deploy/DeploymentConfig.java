package io.mamish.serverbot2.infra.deploy;

import java.util.List;

public class DeploymentConfig {

    private List<ApplicationEnv> environments;

    public List<ApplicationEnv> getEnvironments() {
        return environments;
    }

}
