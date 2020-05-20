package io.mamish.serverbot2.gamemetadata.model;

import io.mamish.serverbot2.dynamomapper.DynamoKey;
import io.mamish.serverbot2.dynamomapper.DynamoKeyType;

public class GameMetadata {

    @DynamoKey(DynamoKeyType.PARTITION)
    private String gameName;
    private String fullName;
    private LaunchState launchState;
    private String instanceQueueUrl;

    public GameMetadata() { }

    public GameMetadata(String gameName, String fullName, LaunchState launchState, String instanceQueueUrl) {
        this.gameName = gameName;
        this.fullName = fullName;
        this.launchState = launchState;
        this.instanceQueueUrl = instanceQueueUrl;
    }

    public String getGameName() {
        return gameName;
    }

    public String getFullName() {
        return fullName;
    }

    public LaunchState getLaunchState() {
        return launchState;
    }

    public String getInstanceQueueUrl() {
        return instanceQueueUrl;
    }

}
