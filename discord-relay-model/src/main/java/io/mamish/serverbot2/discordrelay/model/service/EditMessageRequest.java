package io.mamish.serverbot2.discordrelay.model.service;

import io.mamish.serverbot2.framework.common.ApiArgumentInfo;
import io.mamish.serverbot2.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 0, name = "EditMessage", numRequiredFields = 3,
        description = "Edit a message previously sent with NewMessage, using the previous external ID")
public class EditMessageRequest {

    @ApiArgumentInfo(order = 0, name = "content",
            description = "Content to edit existing message with")
    private String content;
    @ApiArgumentInfo(order = 1, name = "externalId",
            description = "Client-side message ID previously sent in NewMessage `externalId` parameter")
    private String externalId;
    @ApiArgumentInfo(order = 2, name = "editMode",
            description = "Message edit mode: can replace all message content or append to it")
    private EditMode editMode;
    @ApiArgumentInfo(order = 3, name = "createIfMissing",
            description = "Optional: set true to send a new message with this external ID if missing")
    private boolean createIfMissing;

    public EditMessageRequest() {}

    public EditMessageRequest(String content, String externalId, EditMode editMode, boolean createIfMissing) {
        this.content = content;
        this.externalId = externalId;
        this.editMode = editMode;
        this.createIfMissing = createIfMissing;
    }

    public String getExternalId() {
        return externalId;
    }

    public EditMode getEditMode() {
        return editMode;
    }

    public boolean shouldCreateIfMissing() {
        return createIfMissing;
    }

    public String getContent() {
        return content;
    }
}
