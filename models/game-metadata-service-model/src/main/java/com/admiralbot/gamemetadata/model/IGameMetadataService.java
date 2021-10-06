package com.admiralbot.gamemetadata.model;

import com.admiralbot.framework.modelling.ApiAuthType;
import com.admiralbot.framework.modelling.ApiEndpointInfo;
import com.admiralbot.framework.modelling.ApiHttpMethod;

@ApiEndpointInfo(serviceName = "gamemetadata", uriPath = "/", httpMethod = ApiHttpMethod.POST, authType = ApiAuthType.IAM)
public interface IGameMetadataService {

    ListGamesResponse listGames(ListGamesRequest request);
    CreateGameResponse createGame(CreateGameRequest request);
    DescribeGameResponse describeGame(DescribeGameRequest request);
    IdentifyInstanceResponse identifyInstance(IdentifyInstanceRequest request);
    LockGameResponse lockGame(LockGameRequest request);
    UpdateGameResponse updateGame(UpdateGameRequest request);
    DeleteGameResponse deleteGame(DeleteGameRequest request);

}
