package io.mamish.serverbot2.gamemetadata.model;

import io.mamish.serverbot2.framework.common.ApiAuthType;
import io.mamish.serverbot2.framework.common.ApiEndpointInfo;
import io.mamish.serverbot2.framework.common.ApiHttpMethod;

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
