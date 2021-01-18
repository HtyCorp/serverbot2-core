package com.admiralbot.urlshortener.userprefs;

import com.admiralbot.sharedconfig.UrlShortenerConfig;
import com.admiralbot.sharedutil.SdkUtils;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Objects;
import java.util.Optional;

public class PreferencesService {

    private final DynamoDbEnhancedClient client = DynamoDbEnhancedClient.builder()
            .dynamoDbClient(SdkUtils.client(DynamoDbClient.builder()))
            .build();
    private final DynamoDbTable<UserPreferences> table = client.table(UrlShortenerConfig.PREFERENCES_DYNAMO_TABLE_NAME,
            TableSchema.fromBean(UserPreferences.class));

    public void putUserPreferences(UserPreferences preferences) {
        try {
            Objects.requireNonNull(preferences.getUserId());
            if (preferences.isPushEnabled()) {
                Objects.requireNonNull(preferences.getPushSubscription());
            }
            table.putItem(preferences);
        } catch (NullPointerException e) {
            throw new IllegalArgumentException("Required user preference parameters missing");
        }
    }

    public Optional<UserPreferences> getUserPreferences(String userId) {
        Objects.requireNonNull(userId);
        UserPreferences maybePreferences = table.getItem(Key.builder().partitionValue(userId).build());
        return Optional.ofNullable(maybePreferences);
    }



}
