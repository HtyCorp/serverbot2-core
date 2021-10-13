package com.admiralbot.appdaemon;

import com.admiralbot.nativeimagesupport.annotation.RegisterGsonType;

import java.util.List;
import java.util.Map;

@RegisterGsonType
public class GameConfigFile {

    private List<String> launchCommand;
    private boolean relativePath;
    private Map<String,String> environment;

    public List<String> getLaunchCommand() {
        return launchCommand;
    }

    public boolean isRelativePath() {
        return relativePath;
    }

    public Map<String, String> getEnvironment() {
        return environment;
    }

}
