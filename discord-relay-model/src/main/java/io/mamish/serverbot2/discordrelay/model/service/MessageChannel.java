package io.mamish.serverbot2.discordrelay.model.service;

public enum MessageChannel {

    WELCOME,
    SERVERS,
    ADMIN,
    DEBUG;

    public String toLowerCase() {
        return toString().toLowerCase();
    }

}
