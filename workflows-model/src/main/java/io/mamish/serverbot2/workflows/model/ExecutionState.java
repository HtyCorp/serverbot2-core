package io.mamish.serverbot2.workflows.model;

public class ExecutionState {

    // Provided by client starting state machine
    private String requesterDiscordId;
    private String gameName;
    private String initialMessageUuid;
    private String laterMessageUuid;

    // Provided by state machine context
    private Tasks taskName;
    private String taskToken;

    public ExecutionState() { }

    public ExecutionState(String requesterDiscordId, String gameName, String initialMessageUuid, String laterMessageUuid) {
        this.requesterDiscordId = requesterDiscordId;
        this.gameName = gameName;
        this.initialMessageUuid = initialMessageUuid;
        this.laterMessageUuid = laterMessageUuid;
    }

    public ExecutionState(String requesterDiscordId, String gameName, String initialMessageUuid, String laterMessageUuid, Tasks taskName, String taskToken) {
        this.requesterDiscordId = requesterDiscordId;
        this.gameName = gameName;
        this.initialMessageUuid = initialMessageUuid;
        this.laterMessageUuid = laterMessageUuid;
        this.taskName = taskName;
        this.taskToken = taskToken;
    }

    public String getRequesterDiscordId() {
        return requesterDiscordId;
    }

    public String getGameName() {
        return gameName;
    }

    public String getInitialMessageUuid() {
        return initialMessageUuid;
    }

    public String getLaterMessageUuid() {
        return laterMessageUuid;
    }

    public Tasks getTaskName() {
        return taskName;
    }

    public String getTaskToken() {
        return taskToken;
    }
}
