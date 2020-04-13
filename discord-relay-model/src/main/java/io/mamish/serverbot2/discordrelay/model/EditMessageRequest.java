package io.mamish.serverbot2.discordrelay.model;

public class EditMessageRequest {

    private String externalId;
    private EditMode editMode;
    private boolean createIfMissing;
    private String content;

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

    public boolean isCreateIfMissing() {
        return createIfMissing;
    }

    public String getContent() {
        return content;
    }
}
