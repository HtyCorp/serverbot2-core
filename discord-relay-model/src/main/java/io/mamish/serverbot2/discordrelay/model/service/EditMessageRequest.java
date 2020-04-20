package io.mamish.serverbot2.discordrelay.model.service;

public class EditMessageRequest {

    private String externalId;
    private EditMode editMode;
    private boolean createIfMissing;
    private String content;

    public EditMessageRequest() {}

    public EditMessageRequest(String externalId, EditMode editMode, boolean createIfMissing, String content) {
        this.externalId = externalId;
        this.editMode = editMode;
        this.createIfMissing = createIfMissing;
        this.content = content;
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
