package io.mamish.serverbot2.gamemetadata.model;

public interface IGameMetadataService {

    ListGamesResponse listGames(ListGamesRequest request);
    CreateGameResponse createGame(CreateGameRequest request);
    DescribeGameResponse describeGame(DescribeGameRequest request);
    IdentifyInstanceResponse identifyInstance(IdentifyInstanceRequest request);
    LockGameResponse lockGame(LockGameRequest request);
    UpdateGameResponse updateGame(UpdateGameRequest request);
    DeleteGameResponse deleteGame(DeleteGameRequest request);

}
