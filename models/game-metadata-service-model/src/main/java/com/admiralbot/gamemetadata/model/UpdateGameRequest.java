package com.admiralbot.gamemetadata.model;

import com.admiralbot.framework.common.ApiArgumentInfo;
import com.admiralbot.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 7, name = "UpdateGame", numRequiredFields = 1,
        description = "Update certain fields of a game's metadata")
public class UpdateGameRequest {

    @ApiArgumentInfo(order = 0, description = "Name of game to update")
    private String gameName;

    @ApiArgumentInfo(order = 1, description = "Full display name of game")
    private String fullName;

    @ApiArgumentInfo(order = 2, description = "State to set")
    private GameReadyState state;

    @ApiArgumentInfo(order = 3, description = "ID of instance hosting game")
    private String instanceId;

    @ApiArgumentInfo(order = 4, description = "URL of queue the instance listens for messages on")
    private String instanceQueueName;

    @ApiArgumentInfo(order = 5, description = "Task token for current execution state in Step Functions")
    private String taskCompletionToken;

    @ApiArgumentInfo(order = 6, description = "Disable state check, forcing update to work regardless")
    private Boolean bypassStateCheck;

    public UpdateGameRequest() { }

    public UpdateGameRequest(String gameName, String fullName, GameReadyState state, String instanceId, String instanceQueueName, String taskCompletionToken) {
        this.gameName = gameName;
        this.fullName = fullName;
        this.state = state;
        this.instanceId = instanceId;
        this.instanceQueueName = instanceQueueName;
        this.taskCompletionToken = taskCompletionToken;
    }

    public UpdateGameRequest(String gameName, String fullName, GameReadyState state, String instanceId, String instanceQueueName, String taskCompletionToken, boolean bypassStateCheck) {
        this(gameName, fullName, state, instanceId, instanceQueueName, taskCompletionToken);
        this.bypassStateCheck = bypassStateCheck;
    }

    public String getGameName() {
        return gameName;
    }

    public String getFullName() {
        return fullName;
    }

    public GameReadyState getState() {
        return state;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getInstanceQueueName() {
        return instanceQueueName;
    }

    public String getTaskCompletionToken() {
        return taskCompletionToken;
    }

    public Boolean getBypassStateCheck() {
        return bypassStateCheck;
    }
}
