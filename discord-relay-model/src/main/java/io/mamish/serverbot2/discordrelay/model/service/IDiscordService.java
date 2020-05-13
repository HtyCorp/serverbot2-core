package io.mamish.serverbot2.discordrelay.model.service;

public interface IDiscordService {

    NewMessageResponse requestNewMessage(NewMessageRequest newMessageRequest);
    EditMessageResponse requestEditMessage(EditMessageRequest editMessageRequest);

}
