package io.mamish.serverbot2.gamemetadata.model;

public class CreateGameResponse {

    private GameMetadata newMetadata;

    public CreateGameResponse() { }

    public CreateGameResponse(GameMetadata newMetadata) {
        this.newMetadata = newMetadata;
    }

    public GameMetadata getNewMetadata() {
        return newMetadata;
    }

}
