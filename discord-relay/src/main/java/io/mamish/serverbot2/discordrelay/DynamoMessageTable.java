package io.mamish.serverbot2.discordrelay;

import io.mamish.serverbot2.sharedconfig.DiscordConfig;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

public class DynamoMessageTable {

    private final DynamoDbEnhancedClient ddbClient = DynamoDbEnhancedClient.create();
    private final DynamoDbTable<DynamoMessageItem> messageTable = ddbClient.table(DiscordConfig.MESSAGE_TABLE_NAME,
            TableSchema.fromBean(DynamoMessageItem.class));

    public boolean has(String externalId) {
        return consistentGet(externalId) != null;
    }

    public DynamoMessageItem get(String externalId) {
        return consistentGet(externalId);
    }

    public void put(DynamoMessageItem item) {
        messageTable.putItem(r -> r.item(item));
    }

    private DynamoMessageItem consistentGet(String externalId) {
        return messageTable.getItem(r -> r.consistentRead(true).key(partition(externalId)));
    }

    private static Key partition(String value) {
        return Key.builder().partitionValue(value).build();
    }

}