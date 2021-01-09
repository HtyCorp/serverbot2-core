package com.admiralbot.urlshortener.userprefs;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbFlatten;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbImmutable(builder = UserPreferences.Builder.class)
public class UserPreferences {

    private final String userId;
    private final boolean pushEnabled;
    private final WebPushSubscription pushSubscription;
    private final boolean automaticWorkflowEnabled;

    private UserPreferences(Builder builder) {
        this.userId = builder.userId;
        this.pushEnabled = builder.pushEnabled;
        this.pushSubscription = builder.pushSubscription;
        this.automaticWorkflowEnabled = builder.automaticWorkflowEnabled;
    }

    public static Builder builder() {
        return new Builder();
    }

    @DynamoDbPartitionKey
    public String getUserId() {
        return userId;
    }

    public boolean isPushEnabled() {
        return pushEnabled;
    }

    @DynamoDbFlatten
    public WebPushSubscription getPushSubscription() {
        return pushSubscription;
    }

    public boolean isAutomaticWorkflowEnabled() {
        return automaticWorkflowEnabled;
    }

    public static class Builder {
        private String userId;
        private boolean pushEnabled;
        private WebPushSubscription pushSubscription;
        private boolean automaticWorkflowEnabled;

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder pushEnabled(boolean pushEnabled) {
            this.pushEnabled = pushEnabled;
            return this;
        }

        public Builder pushSubscription(WebPushSubscription pushSubscription) {
            this.pushSubscription = pushSubscription;
            return this;
        }

        public Builder automaticWorkflowEnabled(boolean automaticWorkflowEnabled) {
            this.automaticWorkflowEnabled = automaticWorkflowEnabled;
            return this;
        }

        public UserPreferences build() {
            return new UserPreferences(this);
        }

    }

}
