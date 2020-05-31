package io.mamish.serverbot2.gamemetadata.model;

public enum GameReadyState {
    STOPPED,
    BUSY,
    RUNNING;

    public String toLowerCase() {
        return toString().toLowerCase();
    }

}
