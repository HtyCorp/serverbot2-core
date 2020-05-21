package io.mamish.serverbot2.gamemetadata.model;

import io.mamish.serverbot2.framework.common.ApiArgumentInfo;
import io.mamish.serverbot2.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 6, name = "CreateGame", numRequiredFields = 2,
        description = "Create an uninitialised entry in game metadata table")
public class CreateGameRequest {

    @ApiArgumentInfo(order = 0, description = "Short name of game to create")
    private String gameName;
    @ApiArgumentInfo(order = 1, description = "Full name of game to create")
    private String fullName;

    public CreateGameRequest() {
    }

    public CreateGameRequest(String gameName, String fullName) {
        this.gameName = gameName;
        this.fullName = fullName;
    }

    public String getGameName() {
        return gameName;
    }

    public String getFullName() {
        return fullName;
    }
}
