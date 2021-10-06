package com.admiralbot.discordrelay.model.service;

import com.admiralbot.framework.modelling.ApiArgumentInfo;
import com.admiralbot.framework.modelling.ApiRequestInfo;

@ApiRequestInfo(order = 1, name = "EditMessage", numRequiredFields = 3,
        description = "Edit a message previously sent with NewMessage, using the previous external ID")
public class EditMessageRequest {

    @ApiArgumentInfo(order = 0, description = "Content to edit existing message with")
    private String content;
    @ApiArgumentInfo(order = 1, description = "Client-side message ID previously sent in NewMessage `externalId` parameter")
    private String externalId;
    @ApiArgumentInfo(order = 2, description = "Message edit mode: can replace all message content or append to it")
    private EditMode editMode;

    public EditMessageRequest() {}

    public EditMessageRequest(String content, String externalId, EditMode editMode) {
        this.content = content;
        this.externalId = externalId;
        this.editMode = editMode;
    }

    public String getExternalId() {
        return externalId;
    }

    public EditMode getEditMode() {
        return editMode;
    }

    public String getContent() {
        return content;
    }
}
