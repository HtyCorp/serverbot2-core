package io.mamish.serverbot2.appdaemon;

import java.util.List;
import java.util.Map;

public class GameConfigFile {

    private List<String> launchCommand;
    private Map<String,String> environment;

    public List<String> getLaunchCommand() {
        return launchCommand;
    }

    public Map<String, String> getEnvironment() {
        return environment;
    }
}
