package io.mamish.serverbot2.infra.deploy;

import io.mamish.serverbot2.infra.services.*;
import software.amazon.awscdk.core.*;

public class ApplicationStage extends Stage {

    public final AppInstanceShareStack appInstanceShareStack;
    public final CommandStack commandStack;
    public final CommonStack commonStack;
    public final GameMetadataStack gameMetadataStack;
    public final IpAuthorizerStack ipAuthorizerStack;
    public final NetSecStack netSecStack;
    public final ReaperStack reaperStack;
    public final RelayStack relayStack;
    public final WorkflowsStack workflowsStack;

    public ApplicationStage(Construct parent, String id, StageProps props, ApplicationEnv env) {
        super(parent, id, props);

        commonStack = new CommonStack(this, "CommonResources", env);
        ipAuthorizerStack = new IpAuthorizerStack(this, "IpAuthorizerApi", commonStack, env);
        relayStack = new RelayStack(this, "DiscordRelay", commonStack);
        appInstanceShareStack = new AppInstanceShareStack(this, "AppInstanceResources");
        commandStack = new CommandStack(this, "CommandService");
        workflowsStack = new WorkflowsStack(this, "WorkflowService");
        gameMetadataStack = new GameMetadataStack(this, "GameMetadataService");
        netSecStack = new NetSecStack(this, "NetworkSecurityService", commonStack);
        reaperStack = new ReaperStack(this, "ResourceReaper");

    }

}
