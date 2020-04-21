package io.mamish.serverbot2.discordrelay.model.service;

import io.mamish.serverbot2.sharedutil.reflect.ApiArgumentInfo;
import io.mamish.serverbot2.sharedutil.reflect.ApiRequestInfo;

@ApiRequestInfo(order = 0, name = "NewMessage", numRequiredFields = 1,
        description = "Send a message through Discord to the chosen channels or user")
public class NewMessageRequest {

    @ApiArgumentInfo(order = 0, name = "content",
            description = "Message content to send")
    private String content;
    @ApiArgumentInfo(order = 1, name = "externalId",
            description = "Optional external message ID to allow future edits")
    private String externalId;
    @ApiArgumentInfo(order = 2, name = "recipientChannel",
            description = "Application channel to send message to. Mutually exclusive with `recipientUserId`")
    private MessageChannel recipientChannel;
    @ApiArgumentInfo(order = 3, name = "recipientUserId",
            description = "Discord ID of user to send message to. Mutually exclusive with `recipientChannel`")
    private String recipientUserId;

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
}
