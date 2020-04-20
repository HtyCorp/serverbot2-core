package io.mamish.serverbot2.discordrelay.model.service;

public interface IDiscordServiceHandler {

    void onRequestNewMessage(NewMessageRequest newMessageRequest);
    void onRequestEditMessage(EditMessageRequest editMessageRequest);

}
