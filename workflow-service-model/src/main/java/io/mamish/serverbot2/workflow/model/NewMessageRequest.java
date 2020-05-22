package io.mamish.serverbot2.workflow.model;

import io.mamish.serverbot2.framework.common.ApiArgumentInfo;
import io.mamish.serverbot2.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 0, name = "NewMessage", numRequiredFields = 1, description = "Send a new message via Discord")
public class NewMessageRequest {

    @ApiArgumentInfo(order = 0, description = "Content of new message")
    private String messageContent;

    @ApiArgumentInfo(order = 1, description = "Optional ID of new message to allow edits via EditMessage")
    private String messageId;

    public NewMessageRequest() { }

    public NewMessageRequest(String messageContent, String messageId) {
        this.messageContent = messageContent;
        this.messageId = messageId;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public String getMessageId() {
        return messageId;
    }
}
