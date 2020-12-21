package com.admiralbot.gamemetadata.metastore;

import com.admiralbot.gamemetadata.model.GameReadyState;
import com.admiralbot.sharedconfig.GameMetadataConfig;
import com.admiralbot.sharedutil.SdkUtils;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.util.Optional;
import java.util.stream.Stream;

public class DynamoTableMetadataStore implements IMetadataStore {

    private final DynamoDbEnhancedClient ddbClient = DynamoDbEnhancedClient.builder()
            .dynamoDbClient(SdkUtils.client(DynamoDbClient.builder()))
            .build();
    private final DynamoDbTable<GameMetadataBean> table = ddbClient.table(GameMetadataConfig.TABLE_NAME,
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
    public void update(GameMetadataBean item) {
        table.updateItem(r -> r.item(item).ignoreNulls(true));
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
        // Refs:
        // https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.ConditionExpressions.html
        // https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.OperatorsAndFunctions.html

        // Possible SDK bug in DynamoDbTable.deleteItem requires optional attributes to be specified. The expression
        // name otherwise wouldn't be required.
        // Tracking: https://github.com/aws/aws-sdk-java-v2/issues/1913

        String operator = (inStoppedState) ? "=" : "<>";
        return Expression.builder()
                .expression("#state "+operator+" :stopped")
                .putExpressionName("#state", "gameReadyState")
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
