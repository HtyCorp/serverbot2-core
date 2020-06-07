package io.mamish.serverbot2.gamemetadata.model;

public enum GameReadyState {
    STOPPED,
    STARTING,
    RUNNING,
    STOPPING;

    public String toLowerCase() {
        return toString().toLowerCase();
    }

}
