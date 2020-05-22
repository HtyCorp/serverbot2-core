package io.mamish.serverbot2.workflow.model;

import io.mamish.serverbot2.framework.common.ApiArgumentInfo;
import io.mamish.serverbot2.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 0, name = "NewInstance", numRequiredFields = 1, description = "Set up a new game instance")
public class NewInstanceRequest {

    @ApiArgumentInfo(order = 0, description = "Name of game to set up instance for")
    private String gameName;

    public NewInstanceRequest() { }

    public NewInstanceRequest(String gameName) {
        this.gameName = gameName;
    }

    public String getGameName() {
        return gameName;
    }
}
