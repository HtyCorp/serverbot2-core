package com.admiralbot.gamemetadata.model;

import com.admiralbot.framework.common.ApiArgumentInfo;
import com.admiralbot.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 7, name = "DeleteGame", numRequiredFields = 1, description = "Remove a game metadata entry from database")
public class DeleteGameRequest {

    @ApiArgumentInfo(order = 0, description = "Name of game to remove entry for")
    private String gameName;

    public DeleteGameRequest() { }

    public DeleteGameRequest(String gameName) {
        this.gameName = gameName;
    }

    public String getGameName() {
        return gameName;
    }
}
