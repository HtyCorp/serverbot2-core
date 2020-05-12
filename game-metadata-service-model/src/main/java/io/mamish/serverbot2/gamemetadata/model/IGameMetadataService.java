package io.mamish.serverbot2.gamemetadata.model;

public interface IGameMetadataService {

    ListGamesResponse requestListGames(ListGamesRequest request);
    DescribeGameResponse requestDescribeGame(DescribeGameRequest request);

}
