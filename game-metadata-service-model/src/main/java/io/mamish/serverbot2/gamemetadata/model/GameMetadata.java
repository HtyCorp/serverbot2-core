package io.mamish.serverbot2.gamemetadata.model;

public class GameMetadata {

    private String gameName;
    private String fullName;
    private GameReadyState gameReadyState;
    private String instanceId;
    private String instanceQueueName;
    private String taskCompletionToken;

    public GameMetadata() { }

    public GameMetadata(String gameName, String fullName, GameReadyState gameReadyState, String instanceId, String instanceQueueName, String taskCompletionToken) {
        this.gameName = gameName;
        this.fullName = fullName;
        this.gameReadyState = gameReadyState;
        this.instanceId = instanceId;
        this.instanceQueueName = instanceQueueName;
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

    public String getInstanceQueueName() {
        return instanceQueueName;
    }

    public String getTaskCompletionToken() {
        return taskCompletionToken;
    }
}
