package io.mamish.serverbot2.deployinfra;

import io.mamish.serverbot2.sharedconfig.DeploymentConfig;
import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Environment;
import software.amazon.awscdk.core.StackProps;

public class DeploymentApp {
    public static void main(String[] args) {
        App app = new App();

        Environment env = Environment.builder()
                .account(DeploymentConfig.CDK_DEFAULT_ACCOUNT.getValue())
                .region(DeploymentConfig.CDK_DEFAULT_REGION.getValue())
                .build();
        StackProps props = StackProps.builder().env(env).build();

        new DeploymentStack(app, "DeploymentStack", props);

        app.synth();
    }
}
