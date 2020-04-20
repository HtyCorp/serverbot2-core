package io.mamish.serverbot2.discordrelay.model.service;

public interface DiscordRequestHandler {

    void onRequestNewMessage(NewMessageRequest newMessageRequest);
    void onRequestEditMessage(EditMessageRequest editMessageRequest);

}
