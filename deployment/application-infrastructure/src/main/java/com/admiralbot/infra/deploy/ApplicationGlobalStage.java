package com.admiralbot.infra.deploy;

import com.admiralbot.infra.baseline.GlobalCommonStack;
import com.admiralbot.infra.frontend.DeliveryPrefsEditorStack;
import com.admiralbot.infra.frontend.UrlRedirectorStack;
import software.amazon.awscdk.core.Stage;
import software.amazon.awscdk.core.StageProps;
import software.constructs.Construct;

public class ApplicationGlobalStage extends Stage {

    private final ApplicationEnv mainEnv;
    private final GlobalCommonStack globalCommonStack;

    public ApplicationEnv getMainEnv() {
        return mainEnv;
    }

    public GlobalCommonStack getGlobalCommonStack() {
        return globalCommonStack;
    }

    public ApplicationGlobalStage(Construct parent, String id, StageProps props, ApplicationEnv mainEnv) {
        super(parent, id, props);
        this.mainEnv = mainEnv;

        globalCommonStack = new GlobalCommonStack(this, "GlobalCommon");

        new UrlRedirectorStack(this, "UrlShortenerFrontend");
        new DeliveryPrefsEditorStack(this, "DeliverPrefsEditor");

    }
}
