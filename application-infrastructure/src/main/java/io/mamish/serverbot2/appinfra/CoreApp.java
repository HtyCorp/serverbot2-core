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
        new IpStack(app, "IpAuthService", props, commonStack);
        new RelayStack(app, "DiscordRelayService", props);
        new CommandStack(app, "CommandService", props);
        new WorkflowsStack(app, "Workflows", props);
        new GameMetadataStack(app, "GameMetadataService", props);
        new NetSecStack(app, "NetSecService", props);
        new ReaperStack(app, "ResourceReaper", props);

        app.synth();

    }

}
