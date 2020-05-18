package io.mamish.serverbot2.gamemetadata.model;

import io.mamish.serverbot2.sharedutil.reflect.DdbAttribute;
import io.mamish.serverbot2.sharedutil.reflect.DdbKeyType;

public class GameMetadata {

    @DdbAttribute(value = "gameName", keyType = DdbKeyType.PARTITION)
    String gameName;

    @DdbAttribute("fullName")
    String fullName;

    @DdbAttribute("launchState")
    LaunchState launchState;

    @DdbAttribute("sfnExecutionId")
    String sfnExecutionId;

    public GameMetadata() { }

    public GameMetadata(String gameName, String fullName, LaunchState launchState, String sfnExecutionId) {
        this.gameName = gameName;
        this.fullName = fullName;
        this.launchState = launchState;
        this.sfnExecutionId = sfnExecutionId;
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

    public String getSfnExecutionId() {
        return sfnExecutionId;
    }
}
