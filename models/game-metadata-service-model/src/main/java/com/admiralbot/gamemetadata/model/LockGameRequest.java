package com.admiralbot.gamemetadata.model;

import com.admiralbot.framework.modelling.ApiArgumentInfo;
import com.admiralbot.framework.modelling.ApiRequestInfo;

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
