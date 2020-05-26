package io.mamish.serverbot2.gamemetadata;

import io.mamish.serverbot2.framework.exception.server.RequestHandlingException;
import io.mamish.serverbot2.framework.exception.server.RequestValidationException;
import io.mamish.serverbot2.gamemetadata.model.*;
import io.mamish.serverbot2.sharedconfig.GameMetadataConfig;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GameMetadataServiceHandler implements IGameMetadataService {

    private static final Pattern GAME_NAME_REGEX = Pattern.compile("[a-z][a-z0-9]*");

    private static final String ERR_MSG_GAME_LOCKED = "Game in STOPPED state. Must call LockGame to modify it";

    DynamoDbEnhancedClient ddbClient = DynamoDbEnhancedClient.create();
    DynamoDbTable<GameMetadataBean> table = ddbClient.table(GameMetadataConfig.TABLE_NAME,
            TableSchema.fromBean(GameMetadataBean.class));

    @Override
    public ListGamesResponse listGames(ListGamesRequest request) {
        List<GameMetadata> allMetadata = consistentTableScan().stream()
                .map(GameMetadataBean::toModel)
                .collect(Collectors.toList());
        return new ListGamesResponse(allMetadata);
    }

    @Override
    public DescribeGameResponse describeGame(DescribeGameRequest request) {
        String name = request.getGameName();
        validateGameName(name);

        GameMetadataBean result = consistentTableGet(name);
        if (result == null) {
            return new DescribeGameResponse(false, null);
        } else {
            return new DescribeGameResponse(true, result.toModel());
        }
    }

    @Override
    public IdentifyInstanceResponse identifyInstance(IdentifyInstanceRequest request) {
        DynamoDbIndex<GameMetadataBean> index = table.index(GameMetadataBean.INDEX_BY_INSTANCE);
        QueryConditional matchInstanceId = QueryConditional.keyEqualTo(partitionKey(request.getInstanceId()));
        Optional<GameMetadataBean> maybeMetadata = index.query(r -> r.queryConditional(matchInstanceId)).stream()
                .flatMap(p -> p.items().stream())
                .findFirst();
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

        Expression notExistsAlready = Expression.builder()
                .expression("attribute_not_exists(gameName)")
                .build();

        try {
            table.putItem(r -> r.item(newItem).conditionExpression(notExistsAlready));
        } catch (ConditionalCheckFailedException e) {
            throw new RequestHandlingException("Game '" + name + "' already exists");
        }

        return new CreateGameResponse(newItem.toModel());

    }

    @Override
    public LockGameResponse lockGame(LockGameRequest request) {
        String name = request.getGameName();
        validateGameName(name);

        GameMetadataBean currentItem = consistentTableGet(name);
        if (currentItem == null) {
            throw new RequestValidationException("No game '" + name + "' present in table");
        }

        GameMetadataBean updateItem = new GameMetadataBean();
        updateItem.setGameReadyState(GameReadyState.BUSY);

        try {
            table.updateItem(r -> r.item(updateItem).ignoreNulls(true).conditionExpression(conditionIsInStoppedState(true)));
        } catch (ConditionalCheckFailedException e) {
            throw new RequestHandlingException("Game state is invalid for locking");
        }

        return new LockGameResponse();
    }

    @Override
    public UpdateGameResponse updateGame(UpdateGameRequest request) {
        String name = request.getGameName();
        validateGameName(name);

        GameMetadataBean item = consistentTableGet(name);
        if (item == null) {
            throw new RequestValidationException("No game '" + name + "' present in table");
        }
        if (item.getGameReadyState().equals(GameReadyState.STOPPED)) {
            throw new RequestHandlingException(ERR_MSG_GAME_LOCKED);
        }

        // Field changes are implemented in GameMetadataBean to reduce chance of missing fields accidentally.
        item.updateFromApiUpdateRequest(request);

        try {
            table.updateItem(r -> r.item(item).ignoreNulls(true).conditionExpression(conditionIsInStoppedState(false)));
        } catch (ConditionalCheckFailedException e) {
            throw new RequestHandlingException(ERR_MSG_GAME_LOCKED);
        }

        return new UpdateGameResponse(item.toModel());

    }

    @Override
    public DeleteGameResponse deleteGame(DeleteGameRequest request) {
        String name = request.getGameName();
        validateGameName(name);

        try {
            // We do this in addition to the DeleteItem return to make it possible to differentiate between
            // "item in stopped state" and "item doesn't exist" error cases.
            GameMetadataBean existing = consistentTableGet(name);
            if (existing == null) {
                throw new RequestHandlingException("No game '" + name + "' present in table");
            }

            GameMetadataBean deletedItem = table.deleteItem(r ->
                    r.conditionExpression(conditionIsInStoppedState(false))
                    .key(partitionKey(name)));

            return new DeleteGameResponse(deletedItem.toModel());
        } catch (ConditionalCheckFailedException e) {
            e.printStackTrace();
            throw new RequestHandlingException(ERR_MSG_GAME_LOCKED);
        }

    }

    private void validateGameName(String name) throws RequestValidationException {
        if (GAME_NAME_REGEX.matcher(name).matches()) {
            throw new RequestValidationException("Name '" + name + "' is not a valid name (allowed regex: " + GAME_NAME_REGEX.pattern() + ")");
        }
    }

    private Expression conditionIsInStoppedState(boolean inStoppedState) {
        String operator = (inStoppedState) ? "=" : "!=";
        return Expression.builder()
                .expression("gameReadyState "+operator+" :stopped")
                .putExpressionValue(":stopped", mkString(GameReadyState.STOPPED))
                .build();
    }

    private GameMetadataBean consistentTableGet(String gameName) {
        return table.getItem(r -> r.key(partitionKey(gameName)).consistentRead(true));
    }

    private SdkIterable<GameMetadataBean> consistentTableScan() {
        return table.scan(r -> r.consistentRead(true)).items();
    }

    private Key partitionKey(String gameName) {
        return Key.builder().partitionValue(gameName).build();
    }

    private AttributeValue mkString(String s) {
        return AttributeValue.builder().s(s).build();
    }

    private <T extends Enum<T>> AttributeValue mkString(Enum<T> t) {
        return mkString(t.toString());
    }

}
