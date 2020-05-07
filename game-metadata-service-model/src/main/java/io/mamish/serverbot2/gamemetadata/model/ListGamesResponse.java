package io.mamish.serverbot2.gamemetadata.model;

import io.mamish.serverbot2.sharedutil.reflect.ApiArgumentInfo;
import io.mamish.serverbot2.sharedutil.reflect.ApiRequestInfo;

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
