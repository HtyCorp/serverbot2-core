package io.mamish.serverbot2.appinfra;

import io.mamish.serverbot2.sharedconfig.DeploymentConfig;
import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Environment;
import software.amazon.awscdk.core.StackProps;

public class CoreApp {

    public static void main(String[] args) {

        App app = new App();

        Environment env = Environment.builder()
                .account(DeploymentConfig.CDK_DEFAULT_ACCOUNT.getValue())
                .region(DeploymentConfig.CDK_DEFAULT_REGION.getValue())
                .build();
        StackProps props = StackProps.builder().env(env).build();

        CommonStack commonStack = new CommonStack(app, "CommonStack", props);
        new IpStack(app, "IpStack", props, commonStack);
        new RelayStack(app, "RelayStack", props);
        new CommandStack(app, "CommandStack", props);

        app.synth();

    }

}
