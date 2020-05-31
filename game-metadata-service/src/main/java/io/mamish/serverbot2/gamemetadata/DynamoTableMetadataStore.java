package io.mamish.serverbot2.gamemetadata;

import io.mamish.serverbot2.gamemetadata.model.GameReadyState;
import io.mamish.serverbot2.sharedconfig.GameMetadataConfig;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.util.Optional;
import java.util.stream.Stream;

public class DynamoTableMetadataStore implements IMetadataStore {

    DynamoDbEnhancedClient ddbClient = DynamoDbEnhancedClient.create();
    DynamoDbTable<GameMetadataBean> table = ddbClient.table(GameMetadataConfig.TABLE_NAME,
            TableSchema.fromBean(GameMetadataBean.class));

    @Override
    public void putIfMissing(GameMetadataBean item) {
        try {
            Expression notExistsAlready = Expression.builder()
                    .expression("attribute_not_exists(gameName)")
                    .build();
            table.putItem(r -> r.item(item).conditionExpression(notExistsAlready));
        } catch (ConditionalCheckFailedException e) {
            throw new StoreConditionException(e);
        }
    }

    @Override
    public GameMetadataBean get(String key) {
        return table.getItem(r -> r.key(pkey(key)).consistentRead(true));
    }

    @Override
    public Stream<GameMetadataBean> getAll() {
        return table.scan(r -> r.consistentRead(true)).items().stream();
    }

    @Override
    public Optional<GameMetadataBean> getInstanceIdIndex(String instanceId) {
        DynamoDbIndex<GameMetadataBean> index = table.index(GameMetadataBean.INDEX_BY_INSTANCE);
        QueryConditional matchInstanceId = QueryConditional.keyEqualTo(pkey(instanceId));
        return index.query(r -> r.queryConditional(matchInstanceId)).stream()
                .flatMap(p -> p.items().stream())
                .findFirst();
    }

    @Override
    public void updateIfStopped(GameMetadataBean item, boolean isStopped) {
        try {
            table.updateItem(r -> r.item(item).ignoreNulls(true).conditionExpression(conditionIsInStoppedState(isStopped)));
        } catch (ConditionalCheckFailedException e) {
            throw new StoreConditionException(e);
        }
    }

    @Override
    public GameMetadataBean deleteIfStopped(String key, boolean isStopped) {
        try {
            return table.deleteItem(r -> r.key(pkey(key)).conditionExpression(conditionIsInStoppedState(isStopped)));
        } catch (ConditionalCheckFailedException e) {
            throw new StoreConditionException(e);
        }
    }

    private Expression conditionIsInStoppedState(boolean inStoppedState) {
        String operator = (inStoppedState) ? "=" : "!=";
        return Expression.builder()
                .expression("gameReadyState "+operator+" :stopped")
                .putExpressionValue(":stopped", mkString(GameReadyState.STOPPED))
                .build();
    }

    private Key pkey(String gameName) {
        return Key.builder().partitionValue(gameName).build();
    }

    private AttributeValue mkString(String s) {
        return AttributeValue.builder().s(s).build();
    }

    private <T extends Enum<T>> AttributeValue mkString(Enum<T> t) {
        return mkString(t.toString());
    }
}
