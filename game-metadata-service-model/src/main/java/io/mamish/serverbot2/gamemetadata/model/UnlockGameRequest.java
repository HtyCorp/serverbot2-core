package io.mamish.serverbot2.gamemetadata.model;

import io.mamish.serverbot2.framework.common.ApiArgumentInfo;
import io.mamish.serverbot2.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 3, name = "UnlockGame", numRequiredFields = 1, description = "Unlock a game after running it")
public class UnlockGameRequest {

    @ApiArgumentInfo(order = 0, description = "Name of game to unlock")
    private String gameName;

    public UnlockGameRequest() { }

    public UnlockGameRequest(String gameName) {
        this.gameName = gameName;
    }

    public String getGameName() {
        return gameName;
    }
}
