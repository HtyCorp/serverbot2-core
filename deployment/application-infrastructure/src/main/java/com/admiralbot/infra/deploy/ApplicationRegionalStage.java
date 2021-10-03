package com.admiralbot.infra.deploy;

import com.admiralbot.infra.baseline.CommonStack;
import com.admiralbot.infra.baseline.ServiceClusterStack;
import com.admiralbot.infra.services.*;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stage;
import software.amazon.awscdk.core.StageProps;

public class ApplicationRegionalStage extends Stage {

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

    public ApplicationRegionalStage(Construct parent, String id, StageProps props, ApplicationEnv env) {
        super(parent, id, props);

        this.env = env;

        commonStack = new CommonStack(this, "CommonResources");
        serviceClusterStack = new ServiceClusterStack(this, "ServiceCluster");

        // Previously used for native Lambda experimentation, currently disabled.
        // If enabling this, remember to re-enable native-image plugin in echo/pom.xml.
        // new EchoDebugStack(this, "EchoDebugService");

        // Active services
        new UrlShortenerStack(this, "UrlShortenerApi");
        new AppInstanceShareStack(this, "AppInstanceResources");
        new CommandStack(this, "CommandService");
        new WorkflowsStack(this, "WorkflowService");
        new GameMetadataStack(this, "GameMetadataService");
        new NetSecStack(this, "NetworkSecurityService");
        new RelayStack(this, "DiscordRelay");
        new IpAuthorizerStack(this, "IpAuthorizerApi");
        new ReaperStack(this, "ResourceReaper");
    }

}
