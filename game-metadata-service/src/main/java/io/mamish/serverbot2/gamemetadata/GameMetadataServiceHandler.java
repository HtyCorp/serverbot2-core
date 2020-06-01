package io.mamish.serverbot2.gamemetadata;

import io.mamish.serverbot2.framework.exception.server.RequestHandlingException;
import io.mamish.serverbot2.framework.exception.server.RequestValidationException;
import io.mamish.serverbot2.gamemetadata.metastore.*;
import io.mamish.serverbot2.gamemetadata.model.*;
import io.mamish.serverbot2.sharedconfig.CommonConfig;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GameMetadataServiceHandler implements IGameMetadataService {

    private static final String ERR_MSG_GAME_LOCKED = "Game in STOPPED state. Must call LockGame to modify it";

    private final IMetadataStore store = chooseDataStore();

    @Override
    public ListGamesResponse listGames(ListGamesRequest request) {
        List<GameMetadata> allMetadata = store.getAll()
                .map(GameMetadataBean::toModel)
                .collect(Collectors.toList());
        return new ListGamesResponse(allMetadata);
    }

    @Override
    public DescribeGameResponse describeGame(DescribeGameRequest request) {
        String name = request.getGameName();
        validateGameName(name);

        GameMetadataBean result = store.get(name);
        if (result == null) {
            return new DescribeGameResponse(false, null);
        } else {
            return new DescribeGameResponse(true, result.toModel());
        }
    }

    @Override
    public IdentifyInstanceResponse identifyInstance(IdentifyInstanceRequest request) {
        Optional<GameMetadataBean> maybeMetadata = store.getInstanceIdIndex(request.getInstanceId());
        if (maybeMetadata.isPresent()) {
            return new IdentifyInstanceResponse(maybeMetadata.get().toModel());
        } else {
            throw new RequestHandlingException("No game found matching instance ID " + request.getInstanceId());
        }
    }

    @Override
    public CreateGameResponse createGame(CreateGameRequest request) {
        String name = request.getGameName();
        validateGameName(name);

        GameMetadataBean newItem = new GameMetadataBean(
                name,
                request.getFullName(),
                GameReadyState.BUSY,
                null,
                null,
                null
        );

        try {
            store.putIfMissing(newItem);
        } catch (StoreConditionException e) {
            throw new RequestValidationException("Game '" + name + "' already exists");
        }

        return new CreateGameResponse(newItem.toModel());

    }

    @Override
    public LockGameResponse lockGame(LockGameRequest request) {
        String name = request.getGameName();
        validateGameName(name);

        GameMetadataBean currentItem = store.get(name);
        if (currentItem == null) {
            throw new RequestValidationException("No game '" + name + "' present in table");
        }

        GameMetadataBean updateItem = new GameMetadataBean(name);
        updateItem.setGameReadyState(GameReadyState.BUSY);

        try {
            store.updateIfStopped(updateItem, true);
        } catch (StoreConditionException e) {
            throw new RequestHandlingException("Game state is invalid for locking");
        }

        return new LockGameResponse();
    }

    @Override
    public UpdateGameResponse updateGame(UpdateGameRequest request) {
        String name = request.getGameName();
        validateGameName(name);

        GameMetadataBean item = store.get(name);
        if (item == null) {
            throw new RequestValidationException("No game '" + name + "' present in table");
        }
        if (item.getGameReadyState().equals(GameReadyState.STOPPED)) {
            throw new RequestHandlingException(ERR_MSG_GAME_LOCKED);
        }

        // Field changes are implemented in GameMetadataBean to reduce chance of missing fields accidentally.
        item.updateFromApiUpdateRequest(request);

        try {
            store.updateIfStopped(item, false);
        } catch (StoreConditionException e) {
            throw new RequestHandlingException(ERR_MSG_GAME_LOCKED);
        }

        GameMetadata model = item.toModel();
        return new UpdateGameResponse(model);

    }

    @Override
    public DeleteGameResponse deleteGame(DeleteGameRequest request) {
        String name = request.getGameName();
        validateGameName(name);

        try {
            // We do this in addition to the DeleteItem return to make it possible to differentiate between
            // "item in stopped state" and "item doesn't exist" error cases.
            GameMetadataBean existing = store.get(name);
            if (existing == null) {
                throw new RequestValidationException("No game '" + name + "' present in table");
            }

            GameMetadataBean deletedItem = store.deleteIfStopped(name, false);

            return new DeleteGameResponse(deletedItem.toModel());
        } catch (StoreConditionException e) {
            e.printStackTrace();
            throw new RequestHandlingException(ERR_MSG_GAME_LOCKED);
        }

    }

    private void validateGameName(String name) throws RequestValidationException {
        if (name == null) {
            // This is set as a required parameter in basically all actions so it should be caught earlier.
            // Including here anyway since it caused a test failure at one point.
            throw new RequestValidationException("Missing required parameter game name");
        }
        Pattern NAME_REGEX = CommonConfig.APP_NAME_REGEX;
        if (!NAME_REGEX.matcher(name).matches()) {
            throw new RequestValidationException("Name '" + name + "' is not a valid name (allowed regex: " + NAME_REGEX.pattern() + ")");
        }
    }

    private IMetadataStore chooseDataStore() {
        if (CommonConfig.ENABLE_MOCK.notNull()) {
            return new LocalSessionMetadataStore();
        } else {
            return new DynamoTableMetadataStore();
        }
    }

}
