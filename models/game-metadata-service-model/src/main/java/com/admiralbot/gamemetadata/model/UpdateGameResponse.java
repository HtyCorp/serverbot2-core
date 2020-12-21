package com.admiralbot.gamemetadata.model;

public class UpdateGameResponse {

    private GameMetadata newMetadata;

    public UpdateGameResponse() { }

    public UpdateGameResponse(GameMetadata newMetadata) {
        this.newMetadata = newMetadata;
    }

    public GameMetadata getNewMetadata() {
        return newMetadata;
    }
}
