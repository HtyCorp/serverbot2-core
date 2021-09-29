package com.admiralbot.infra.services;

import com.admiralbot.echoservice.model.IEchoService;
import com.admiralbot.gamemetadata.model.IGameMetadataService;
import com.admiralbot.infra.constructs.NativeLambdaMicroservice;
import com.admiralbot.infra.constructs.ServiceApi;
import com.admiralbot.infra.deploy.ApplicationRegionalStage;
import com.admiralbot.infra.util.ManagedPolicies;
import com.admiralbot.infra.util.Permissions;
import software.amazon.awscdk.core.Stack;

public class EchoDebugStack extends Stack {

    public EchoDebugStack(ApplicationRegionalStage parent, String id) {
        super(parent, id);

        NativeLambdaMicroservice service = new NativeLambdaMicroservice(this, "Service", parent,
                "echo-debug-service");

        Permissions.addManagedPoliciesToRole(service.getRole(), ManagedPolicies.EC2_READ_ONLY_ACCESS);
        Permissions.addExecuteApi(this, service, IGameMetadataService.class);

        ServiceApi api = new ServiceApi(this, "Api", parent, IEchoService.class);
    }

}
