package io.mamish.serverbot2.gamemetadata.model;

public interface IGameMetadataService {

    ListGamesResponse listGames(ListGamesRequest request);
    DescribeGameResponse describeGame(DescribeGameRequest request);
    CreateGameResponse createGame(CreateGameRequest request);
    LockGameResponse lockGame(LockGameRequest request);
    UpdateGameResponse updateGame(UpdateGameRequest request);
    DeleteGameResponse deleteGame(DeleteGameRequest request);

}
