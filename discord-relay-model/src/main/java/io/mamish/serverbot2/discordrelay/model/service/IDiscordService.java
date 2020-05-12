package io.mamish.serverbot2.discordrelay.model.service;

public interface IDiscordService {

    void requestNewMessage(NewMessageRequest newMessageRequest);
    void requestEditMessage(EditMessageRequest editMessageRequest);

}
