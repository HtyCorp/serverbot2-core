package io.mamish.serverbot2.discordrelay.model.service;

public enum MessageChannel {

    WELCOME,
    MAIN,
    ADMIN;

    public String toLowerCase() {
        return toString().toLowerCase();
    }

}
