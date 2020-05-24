package io.mamish.serverbot2.workflow.model;

public class ExecutionState {

    // Provided by client starting state machine
    private String requesterDiscordId;
    private String gameName;
    private String randomId1;
    private String randomId2;

    // Provided by state machine context
    private MachineTaskNames taskName;
    private String taskToken;

    public ExecutionState() { }

    public ExecutionState(String requesterDiscordId, String gameName, String randomId1, String randomId2) {
        this.requesterDiscordId = requesterDiscordId;
        this.gameName = gameName;
        this.randomId1 = randomId1;
        this.randomId2 = randomId2;
    }

    public String getRequesterDiscordId() {
        return requesterDiscordId;
    }

    public String getGameName() {
        return gameName;
    }

    public String getRandomId1() {
        return randomId1;
    }

    public String getRandomId2() {
        return randomId2;
    }

    public MachineTaskNames getTaskName() {
        return taskName;
    }

    public String getTaskToken() {
        return taskToken;
    }
}
