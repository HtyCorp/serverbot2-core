package io.mamish.serverbot2.infra.app;

import io.mamish.serverbot2.infra.core.*;
import io.mamish.serverbot2.sharedconfig.DeploymentConfig;
import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Environment;
import software.amazon.awscdk.core.StackProps;

public class ServerbotFullApp {

    public static void main(String[] args) {
        new ServerbotFullApp();
    }

    public final App cdkApp;
    public final StackProps coreStackProps;

    public final AppInstanceShareStack appInstanceShareStack;
    public final CommandStack commandStack;
    public final CommonStack commonStack;
    public final GameMetadataStack gameMetadataStack;
    public final IpStack ipStack;
    public final NetSecStack netSecStack;
    public final ReaperStack reaperStack;
    public final RelayStack relayStack;
    public final WorkflowsStack workflowsStack;

    public ServerbotFullApp() {

        cdkApp = new App();
        Environment env = Environment.builder()
                .account(DeploymentConfig.CDK_DEFAULT_ACCOUNT.getValue())
                .region(DeploymentConfig.CDK_DEFAULT_REGION.getValue())
                .build();
        coreStackProps = StackProps.builder().env(env).build();

        commonStack = new CommonStack(cdkApp, "CommonResources", coreStackProps);
        ipStack = new  IpStack(cdkApp, "IpAuthService", coreStackProps, commonStack);
        relayStack = new RelayStack(cdkApp, "DiscordRelay", coreStackProps, commonStack);
        appInstanceShareStack = new AppInstanceShareStack(cdkApp, "AppInstanceResources", coreStackProps);
        commandStack = new CommandStack(cdkApp, "CommandService", coreStackProps);
        workflowsStack = new WorkflowsStack(cdkApp, "WorkflowService", coreStackProps);
        gameMetadataStack = new GameMetadataStack(cdkApp, "GameMetadataService", coreStackProps);
        netSecStack = new NetSecStack(cdkApp, "NetSecService", coreStackProps);
        reaperStack = new ReaperStack(cdkApp, "ResourceReaper", coreStackProps);

        // Disabled for now since appdelivery doesn't support stacks with assets
        // new DeploymentStack(this, "DeploymentPipeline");

        cdkApp.synth();

    }

}
