package io.mamish.serverbot2.gamemetadata.model;

public interface IGameMetadataService {

    ListGamesResponse listGames(ListGamesRequest request);
    DescribeGameResponse describeGame(DescribeGameRequest request);
    LockGameResponse lockGame(LockGameRequest request);

}
