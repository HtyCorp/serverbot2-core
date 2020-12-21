package com.admiralbot.commandservice.model;

import com.admiralbot.discordrelay.model.service.MessageChannel;
import com.admiralbot.framework.common.ApiArgumentInfo;
import com.admiralbot.framework.common.ApiRequestInfo;

import java.util.List;

@ApiRequestInfo(order = 0, name = "ProcessUserCommand", numRequiredFields = 5, description = "Run a user command submitted from Discord")
public class ProcessUserCommandRequest {

    @ApiArgumentInfo(order = 0, description = "List of whitespace-split words in user command")
    private List<String> words;
    @ApiArgumentInfo(order = 1, description = "channel command was sent to")
    private MessageChannel channel;
    @ApiArgumentInfo(order = 2, description = "Discord ID of message containing command")
    private String messageId;
    @ApiArgumentInfo(order = 3, description = "ID of Discord user sending command")
    private String senderId;
    @ApiArgumentInfo(order = 4, description = "Name (with discriminator) of Discord user sending command")
    private String senderName;

    @SuppressWarnings("unused")
    public ProcessUserCommandRequest() {}

    public ProcessUserCommandRequest(List<String> words, MessageChannel channel, String messageId, String senderId, String senderName) {
        this.words = words;
        this.channel = channel;
        this.messageId = messageId;
        this.senderId = senderId;
        this.senderName = senderName;
    }

    public List<String> getWords() {
        return words;
    }

    public MessageChannel getChannel() {
        return channel;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getSenderName() {
        return senderName;
    }
}
