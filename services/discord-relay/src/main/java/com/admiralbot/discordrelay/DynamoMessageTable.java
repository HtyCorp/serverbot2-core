package com.admiralbot.discordrelay;

import com.admiralbot.sharedconfig.DiscordConfig;
import com.admiralbot.sharedutil.SdkUtils;
import com.admiralbot.sharedutil.annotation.ForceClassInitializeAtBuildTime;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@ForceClassInitializeAtBuildTime
public class DynamoMessageTable {

    // Initialised at native-image build time to avoid runtime reflection
    private static final TableSchema<DynamoMessageItem> beanSchema = TableSchema.fromBean(DynamoMessageItem.class);

    private final DynamoDbEnhancedClient ddbClient = DynamoDbEnhancedClient.builder()
            .dynamoDbClient(SdkUtils.client(DynamoDbClient.builder()))
            .build();
    private final DynamoDbTable<DynamoMessageItem> messageTable = ddbClient.table(DiscordConfig.MESSAGE_TABLE_NAME,
            beanSchema);

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
