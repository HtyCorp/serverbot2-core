package io.mamish.serverbot2.gamemetadata;

import io.mamish.serverbot2.framework.exception.server.RequestHandlingException;
import io.mamish.serverbot2.framework.exception.server.RequestValidationException;
import io.mamish.serverbot2.gamemetadata.model.*;
import io.mamish.serverbot2.sharedconfig.GameMetadataConfig;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GameMetadataServiceHandler implements IGameMetadataService {

    private static final Pattern GAME_NAME_REGEX = Pattern.compile("[a-z0-9]+");

    DynamoDbEnhancedClient ddbClient = DynamoDbEnhancedClient.create();
    DynamoDbTable<GameMetadataBean> table = ddbClient.table(GameMetadataConfig.TABLE_NAME,
            TableSchema.fromBean(GameMetadataBean.class));

    @Override
    public ListGamesResponse listGames(ListGamesRequest request) {
        List<GameMetadata> allMetadata = table.scan().items().stream()
                .map(GameMetadataBean::toModel)
                .collect(Collectors.toList());
        return new ListGamesResponse(allMetadata);
    }

    @Override
    public DescribeGameResponse describeGame(DescribeGameRequest request) {
        String name = request.getGameName();
        validateGameName(name);

        GameMetadataBean result = table.getItem(partitionKey(name));
        if (result == null) {
            return new DescribeGameResponse(false, null);
        } else {
            return new DescribeGameResponse(true, result.toModel());
        }
    }

    @Override
    public CreateGameResponse createGame(CreateGameRequest request) {
        String name = request.getGameName();
        validateGameName(name);

        GameMetadataBean newItem = new GameMetadataBean(
                name,
                request.getFullName(),
                GameReadyState.BUSY_WORKING,
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

        GameMetadataBean currentItem = table.getItem(partitionKey(name));
        if (currentItem == null) {
            throw new RequestValidationException("No game '" + name + "' present in table");
        }

        GameMetadataBean updateItem = new GameMetadataBean();
        updateItem.setGameReadyState(GameReadyState.RUNNING);

        Expression checkIsStopped = Expression.builder()
                .expression("gameReadyState = :stopped")
                .putExpressionValue(":stopped", mkString(GameReadyState.READY_TO_RUN))
                .build();

        try {
            table.updateItem(r -> r.item(updateItem).ignoreNulls(true).conditionExpression(checkIsStopped));
        } catch (ConditionalCheckFailedException e) {
            throw new RequestHandlingException("Game state is invalid for locking");
        }

        return new LockGameResponse();
    }

    @Override
    public UpdateGameResponse updateGame(UpdateGameRequest request) {
        String LOCKED_ERR_MSG = "Table item in STOPPED state. Must call LockGame to update it";

        String name = request.getGameName();
        validateGameName(name);

        GameMetadataBean item = table.getItem(partitionKey(request.getGameName()));
        if (item == null) {
            throw new RequestValidationException("No game '" + name + "' present in table");
        }
        if (item.getGameReadyState().equals(GameReadyState.READY_TO_RUN)) {
            throw new RequestHandlingException(LOCKED_ERR_MSG);
        }

        // Field changes are implemented in GameMetadataBean to reduce chance of missing fields accidentally.
        item.updateFromApiUpdateRequest(request);

        Expression checkIsNotStopped = Expression.builder()
                .expression("gameReadyState != :stopped")
                .putExpressionValue(":stopped", mkString(GameReadyState.READY_TO_RUN))
                .build();

        try {
            table.updateItem(r -> r.item(item).ignoreNulls(true).conditionExpression(checkIsNotStopped));
        } catch (ConditionalCheckFailedException e) {
            throw new RequestHandlingException(LOCKED_ERR_MSG);
        }

        return new UpdateGameResponse(item.toModel());

    }

    private <T> void updateIfNotNull(Supplier<T> getter, Consumer<T> setter) {
        T value = getter.get();
        if (value != null) {
            setter.accept(value);
        }
    }

    private void validateGameName(String name) throws RequestValidationException {
        if (GAME_NAME_REGEX.matcher(name).matches()) {
            throw new RequestValidationException("Name '" + name + "' is not a valid name (allowed regex: " + GAME_NAME_REGEX.pattern() + ")");
        }
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
