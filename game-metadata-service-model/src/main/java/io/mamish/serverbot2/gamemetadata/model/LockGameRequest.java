package io.mamish.serverbot2.gamemetadata.model;

import io.mamish.serverbot2.framework.common.ApiArgumentInfo;
import io.mamish.serverbot2.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 2, name = "LockGame", numRequiredFields = 2, description = "Lock a game so it can be started")
public class LockGameRequest {

    @ApiArgumentInfo(order = 0, description = "Name of game to lock")
    private String gameName;

    @ApiArgumentInfo(order = 1, description = "ID of Sfn state machine execution that will be managing the game")
    private String executionId;

    public LockGameRequest() { }

    public LockGameRequest(String gameName, String executionId) {
        this.gameName = gameName;
        this.executionId = executionId;
    }

    public String getGameName() {
        return gameName;
    }

    public String getExecutionId() {
        return executionId;
    }
}
