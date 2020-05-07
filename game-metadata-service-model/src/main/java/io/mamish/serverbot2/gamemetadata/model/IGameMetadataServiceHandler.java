package io.mamish.serverbot2.gamemetadata.model;

public interface IGameMetadataServiceHandler {

    ListGamesResponse onRequestListGames(ListGamesRequest request);
    DescribeGameResponse onRequestDescribeGame(DescribeGameRequest request);

}
