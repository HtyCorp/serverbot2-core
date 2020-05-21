package io.mamish.serverbot2.gamemetadata.model;

public class GameMetadata {

    private String gameName;
    private String fullName;
    private GameReadyState gameReadyState;
    private String instanceId;
    private String instanceQueueUrl;
    private String taskCompletionToken;

    public GameMetadata() { }

    public GameMetadata(String gameName, String fullName, GameReadyState gameReadyState, String instanceId, String instanceQueueUrl, String taskCompletionToken) {
        this.gameName = gameName;
        this.fullName = fullName;
        this.gameReadyState = gameReadyState;
        this.instanceId = instanceId;
        this.instanceQueueUrl = instanceQueueUrl;
        this.taskCompletionToken = taskCompletionToken;
    }

    public String getGameName() {
        return gameName;
    }

    public String getFullName() {
        return fullName;
    }

    public GameReadyState getGameReadyState() {
        return gameReadyState;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getInstanceQueueUrl() {
        return instanceQueueUrl;
    }

    public String getTaskCompletionToken() {
        return taskCompletionToken;
    }
}
