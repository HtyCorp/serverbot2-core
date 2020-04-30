package io.mamish.serverbot2.discordrelay;

import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedconfig.DiscordConfig;
import io.mamish.serverbot2.sharedutil.reflect.SimpleDynamoDbMapper;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.lambda.model.ResourceNotFoundException;

import java.util.Map;

public class MessageDynamoTable {

    private final static String TABLE = DiscordConfig.MESSAGE_TABLE_NAME;

    private DynamoDbClient ddbClient = DynamoDbClient.builder().region(CommonConfig.REGION).build();
    private SimpleDynamoDbMapper<DynamoMessageItem> mapper = new SimpleDynamoDbMapper<>(TABLE, DynamoMessageItem.class);

    public DynamoMessageItem getItem(String externalMessageId) {
        try {
            var keyMap =  Map.of(DiscordConfig.MESSAGE_TABLE_PKEY, AttributeValue.builder().s(externalMessageId).build());
            var attributes = ddbClient.getItem(r -> r.tableName(TABLE).key(keyMap).consistentRead(true)).item();
            return mapper.fromAttributes(attributes);
        } catch (ResourceNotFoundException e) {
            return null;
        }
    }

    public void putItem(DynamoMessageItem item) {
        ddbClient.putItem(r -> r.tableName(TABLE).item(mapper.toAttributes(item)));
    }

}
