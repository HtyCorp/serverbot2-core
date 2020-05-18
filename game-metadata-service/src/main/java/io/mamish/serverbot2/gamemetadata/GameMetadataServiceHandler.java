package io.mamish.serverbot2.gamemetadata;

import io.mamish.serverbot2.gamemetadata.model.*;
import io.mamish.serverbot2.sharedconfig.GameMetadataConfig;
import io.mamish.serverbot2.sharedutil.reflect.SimpleDynamoDbMapper;

import java.util.List;

public class GameMetadataServiceHandler implements IGameMetadataService {

    private final SimpleDynamoDbMapper<GameMetadata> mapper
            = new SimpleDynamoDbMapper<>(GameMetadataConfig.BACKING_TABLE_NAME, GameMetadata.class);

    @Override
    public ListGamesResponse listGames(ListGamesRequest request) {
        List<GameMetadata> allMetadata = mapper.scan();
        return new ListGamesResponse(allMetadata);
    }

    @Override
    public DescribeGameResponse describeGame(DescribeGameRequest request) {
        GameMetadata result = mapper.get(request.getGameName());
        if (result == null) {
            return new DescribeGameResponse(false, null);
        } else {
            return new DescribeGameResponse(true, result);
        }
    }

    @Override
    public LockGameResponse lockGame(LockGameRequest request) {
        return null;
    }

    @Override
    public UnlockGameResponse unlockGame(UnlockGameRequest request) {
        return null;
    }
}
