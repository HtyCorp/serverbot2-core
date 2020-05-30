package io.mamish.serverbot2.gamemetadata.model;

public class DescribeGameResponse {

    private boolean isPresent;
    private GameMetadata game;

    public DescribeGameResponse() { }

    public DescribeGameResponse(boolean isPresent, GameMetadata game) {
        this.isPresent = isPresent;
        this.game = game;
    }

    public boolean isPresent() {
        return isPresent;
    }

    public GameMetadata getGame() {
        return game;
    }
}
