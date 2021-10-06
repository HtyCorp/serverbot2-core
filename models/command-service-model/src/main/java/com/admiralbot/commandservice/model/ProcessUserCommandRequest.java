package com.admiralbot.commandservice.model;

import com.admiralbot.discordrelay.model.service.MessageChannel;
import com.admiralbot.framework.modelling.ApiArgumentInfo;
import com.admiralbot.framework.modelling.ApiRequestInfo;

import java.util.List;

@ApiRequestInfo(order = 0, name = "ProcessUserCommand", numRequiredFields = 5, description = "Run a user command submitted from Discord")
public class ProcessUserCommandRequest {

    @ApiArgumentInfo(order = 0, description = "List of whitespace-split words in user command")
    private List<String> words;
    @ApiArgumentInfo(order = 1, description = "channel command was sent to")
    private MessageChannel channel;
    @ApiArgumentInfo(order = 2, description = "Unique ID of source command was requested from (e.g. interaction ID)")
    private String commandSourceId;
    @ApiArgumentInfo(order = 3, description = "ID of Discord user sending command")
    private String senderId;
    @ApiArgumentInfo(order = 4, description = "Name (with discriminator) of Discord user sending command")
    private String senderName;

    @SuppressWarnings("unused")
    public ProcessUserCommandRequest() {}

    public ProcessUserCommandRequest(List<String> words, MessageChannel channel, String commandSourceId,
                                     String senderId, String senderName) {
        this.words = words;
        this.channel = channel;
        this.commandSourceId = commandSourceId;
        this.senderId = senderId;
        this.senderName = senderName;
    }

    public List<String> getWords() {
        return words;
    }

    public MessageChannel getChannel() {
        return channel;
    }

    public String getCommandSourceId() {
        return commandSourceId;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getSenderName() {
        return senderName;
    }
}
