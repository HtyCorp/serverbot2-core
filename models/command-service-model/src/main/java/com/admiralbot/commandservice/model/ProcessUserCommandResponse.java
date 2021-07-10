package com.admiralbot.commandservice.model;

import com.admiralbot.discordrelay.model.service.SimpleEmbed;

public class ProcessUserCommandResponse {

    // All fields optional
    private String messageContent;
    private String messageExternalId;
    private boolean ephemeralMessage; // Default false is fine since it doesn't change historic behaviour
    private String privateMessageContent;
    private SimpleEmbed privateMessageEmbed;

    @SuppressWarnings("unused")
    public ProcessUserCommandResponse() { }

    public ProcessUserCommandResponse(String messageContent) {
        this.messageContent = messageContent;
    }

    public ProcessUserCommandResponse(String messageContent, String messageExternalId) {
        this.messageContent = messageContent;
        this.messageExternalId = messageExternalId;
    }

    public ProcessUserCommandResponse(String messageContent, boolean ephemeralMessage) {
        this.messageContent = messageContent;
        this.ephemeralMessage = ephemeralMessage;
    }

    public ProcessUserCommandResponse(String messageContent, String privateMessageContent,
                                      SimpleEmbed privateMessageEmbed) {
        this.messageContent = messageContent;
        this.privateMessageContent = privateMessageContent;
        this.privateMessageEmbed = privateMessageEmbed;
    }

    public ProcessUserCommandResponse(String messageContent, boolean ephemeralMessage, String privateMessageContent,
                                      SimpleEmbed privateMessageEmbed) {
        this.messageContent = messageContent;
        this.ephemeralMessage = ephemeralMessage;
        this.privateMessageContent = privateMessageContent;
        this.privateMessageEmbed = privateMessageEmbed;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public String getMessageExternalId() {
        return messageExternalId;
    }

    public boolean isEphemeralMessage() {
        return ephemeralMessage;
    }

    public String getPrivateMessageContent() {
        return privateMessageContent;
    }

    public SimpleEmbed getPrivateMessageEmbed() {
        return privateMessageEmbed;
    }

}
