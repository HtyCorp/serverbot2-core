package com.admiralbot.discordrelay.model.service;

import com.admiralbot.framework.modelling.ApiArgumentInfo;
import com.admiralbot.framework.modelling.ApiRequestInfo;

@ApiRequestInfo(order = 0, name = "NewMessage", numRequiredFields = 1,
        description = "Send a message through Discord to the chosen channels or user")
public class NewMessageRequest {

    @ApiArgumentInfo(order = 0, description = "Message content to send")
    private String content;
    @ApiArgumentInfo(order = 1, description = "Optional external message ID to allow future edits")
    private String externalId;
    @ApiArgumentInfo(order = 2, description = "Application channel to send message to. Mutually exclusive with `recipientUserId`")
    private MessageChannel recipientChannel;
    @ApiArgumentInfo(order = 3, description = "Discord ID of user to send message to. Mutually exclusive with `recipientChannel`")
    private String recipientUserId;
    @ApiArgumentInfo(order = 4, description = "A simple embed object to send with the message")
    private SimpleEmbed embed;
    @ApiArgumentInfo(order = 5, description = "Arbitrary channel ID to send message to.")
    private String recipientChannelId;

    public NewMessageRequest() {}

    public NewMessageRequest(String content, String externalId, MessageChannel recipientChannel, String recipientUserId) {
        this.content = content;
        this.externalId = externalId;
        this.recipientChannel = recipientChannel;
        this.recipientUserId = recipientUserId;
    }

    public String getExternalId() {
        return externalId;
    }

    public MessageChannel getRecipientChannel() {
        return recipientChannel;
    }

    public String getRecipientUserId() {
        return recipientUserId;
    }

    public String getContent() {
        return content;
    }

    public SimpleEmbed getEmbed() {
        return embed;
    }

    public String getRecipientChannelId() {
        return recipientChannelId;
    }
}
