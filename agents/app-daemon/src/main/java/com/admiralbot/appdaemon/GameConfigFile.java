package com.admiralbot.appdaemon;

import java.util.List;
import java.util.Map;

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
