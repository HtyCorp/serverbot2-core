package io.mamish.serverbot2.gamemetadata.model;

public class IdentifyInstanceResponse {

    private GameMetadata metadata;

    public IdentifyInstanceResponse() { }

    public IdentifyInstanceResponse(GameMetadata metadata) {
        this.metadata = metadata;
    }

    public GameMetadata getMetadata() {
        return metadata;
    }
}
