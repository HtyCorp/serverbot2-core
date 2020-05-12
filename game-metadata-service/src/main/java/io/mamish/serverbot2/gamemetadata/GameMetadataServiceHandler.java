package io.mamish.serverbot2.gamemetadata;

import io.mamish.serverbot2.gamemetadata.model.*;
import io.mamish.serverbot2.sharedconfig.GameMetadataConfig;
import io.mamish.serverbot2.sharedutil.reflect.SimpleDynamoDbMapper;

import java.util.List;

public class GameMetadataServiceHandler implements IGameMetadataService {

    private SimpleDynamoDbMapper<GameMetadata> mapper;

    public GameMetadataServiceHandler() {
        mapper = new SimpleDynamoDbMapper<>(GameMetadataConfig.BACKING_TABLE_NAME, GameMetadata.class);
    }

    @Override
    public ListGamesResponse requestListGames(ListGamesRequest request) {
        List<GameMetadata> allMetadata = mapper.scan();
        return new ListGamesResponse(allMetadata);
    }

    @Override
    public DescribeGameResponse requestDescribeGame(DescribeGameRequest request) {
        GameMetadata result = mapper.get(request.getGameName());
        if (result == null) {
            return new DescribeGameResponse(false, null);
        } else {
            return new DescribeGameResponse(true, result);
        }
    }
}
