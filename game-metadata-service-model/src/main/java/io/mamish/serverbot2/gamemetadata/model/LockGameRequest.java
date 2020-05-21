package io.mamish.serverbot2.gamemetadata.model;

import io.mamish.serverbot2.framework.common.ApiArgumentInfo;
import io.mamish.serverbot2.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 2, name = "LockGame", numRequiredFields = 1, description = "Lock a game so it can be started")
public class LockGameRequest {

    @ApiArgumentInfo(order = 0, description = "Name of game to lock")
    private String gameName;

    public LockGameRequest() { }

    public LockGameRequest(String gameName) {
        this.gameName = gameName;
    }

    public String getGameName() {
        return gameName;
    }

}
