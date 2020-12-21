package com.admiralbot.gamemetadata.model;

public enum GameReadyState {
    STOPPED,
    STARTING,
    RUNNING,
    STOPPING;

    public String toLowerCase() {
        return toString().toLowerCase();
    }

}
