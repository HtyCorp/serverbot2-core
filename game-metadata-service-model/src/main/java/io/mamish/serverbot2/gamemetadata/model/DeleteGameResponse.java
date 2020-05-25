package io.mamish.serverbot2.gamemetadata.model;

public class DeleteGameResponse {

    private GameMetadata lastMetadata;

    public DeleteGameResponse() { }

    public DeleteGameResponse(GameMetadata lastMetadata) {
        this.lastMetadata = lastMetadata;
    }

    public GameMetadata getLastMetadata() {
        return lastMetadata;
    }
}
