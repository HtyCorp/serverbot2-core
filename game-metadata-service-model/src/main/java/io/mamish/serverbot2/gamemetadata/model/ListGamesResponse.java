package io.mamish.serverbot2.gamemetadata.model;

import java.util.List;

public class ListGamesResponse {

    private List<GameMetadata> games;

    public ListGamesResponse(List<GameMetadata> games) {
        this.games = games;
    }

    public List<GameMetadata> getGames() {
        return games;
    }
}
