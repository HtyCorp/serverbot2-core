package io.mamish.serverbot2.infra.deploy;

import io.mamish.serverbot2.infra.services.*;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stage;
import software.amazon.awscdk.core.StageProps;

public class ApplicationStage extends Stage {

    private final ApplicationEnv env;
    private final CommonStack commonStack;
    private final ServiceClusterStack serviceClusterStack;

    public ApplicationEnv getEnv() {
        return env;
    }

    public CommonStack getCommonResources() {
        return commonStack;
    }

    public ServiceClusterStack getServiceCluster() {
        return serviceClusterStack;
    }

    public ApplicationStage(Construct parent, String id, StageProps props, ApplicationEnv env) {
        super(parent, id, props);

        this.env = env;

        commonStack = new CommonStack(this, "CommonResources");
        serviceClusterStack = new ServiceClusterStack(this, "ServiceCluster");

        new IpAuthorizerStack(this, "IpAuthorizerApi");
        new UrlShortenerStack(this, "UrlShortenerApi");
        new RelayStack(this, "DiscordRelay");
        new AppInstanceShareStack(this, "AppInstanceResources");
        new CommandStack(this, "CommandService");
        new WorkflowsStack(this, "WorkflowService");
        new GameMetadataStack(this, "GameMetadataService");
        new NetSecStack(this, "NetworkSecurityService");
        new ReaperStack(this, "ResourceReaper");
        new LambdaWarmerStack(this, "LambdaWarmer");

    }

}
