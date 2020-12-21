package com.admiralbot.gamemetadata.model;

import com.admiralbot.framework.common.ApiArgumentInfo;
import com.admiralbot.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 8, name = "IdentifyInstance", numRequiredFields = 1,
        description = "Find the game metadata for a given host instance ID")
public class IdentifyInstanceRequest {

    @ApiArgumentInfo(order = 0, description = "ID of game host instance (e.g. retrieved by instance from its metadata)")
    private String instanceId;

    public IdentifyInstanceRequest() { }

    public IdentifyInstanceRequest(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getInstanceId() {
        return instanceId;
    }

}
