package io.mamish.serverbot2.gamemetadata;

import io.mamish.serverbot2.gamemetadata.model.GameMetadata;
import io.mamish.serverbot2.gamemetadata.model.GameReadyState;
import io.mamish.serverbot2.gamemetadata.model.UpdateGameRequest;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

import java.util.function.Consumer;
import java.util.function.Supplier;

@DynamoDbBean
public class GameMetadataBean {

    private String gameName;
    private String fullName;
    private GameReadyState gameReadyState;
    private String instanceId;
    private String instanceQueueName;
    private String taskCompletionToken;

    public GameMetadataBean() { }

    public GameMetadataBean(String gameName, String fullName, GameReadyState gameReadyState, String instanceId, String instanceQueueName, String taskCompletionToken) {
        this.gameName = gameName;
        this.fullName = fullName;
        this.gameReadyState = gameReadyState;
        this.instanceId = instanceId;
        this.instanceQueueName = instanceQueueName;
        this.taskCompletionToken = taskCompletionToken;
    }

    // Method included here so I'll remember to add field changes if anything in the bean changes.
    // I'll probably forget if it need to be done separately in GameMetadataServiceHandler.
    public void updateFromApiUpdateRequest(UpdateGameRequest request) {
        // Game name is NOT updated: it's a unmodifiable ID.
        setIfNotNull(request::getFullName, this::setFullName);
        setIfNotNull(request::getState, this::setGameReadyState);
        setIfNotNull(request::getInstanceId, this::setInstanceId);
        setIfNotNull(request::getInstanceQueueName, this::setInstanceQueueName);
        setIfNotNull(request::getTaskCompletionToken, this::setTaskCompletionToken);
    }

    private <T> void setIfNotNull(Supplier<T> getter, Consumer<T> setter) {
        T value = getter.get();
        if (value != null) {
            setter.accept(value);
        }
    }

    @DynamoDbPartitionKey
    public String getGameName() {
        return gameName;
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public GameReadyState getGameReadyState() {
        return gameReadyState;
    }

    public void setGameReadyState(GameReadyState gameReadyState) {
        this.gameReadyState = gameReadyState;
    }

    public static final String INDEX_BY_INSTANCE = "allByInstanceId";
    @DynamoDbSecondaryPartitionKey(indexNames = {INDEX_BY_INSTANCE})
    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getInstanceQueueName() {
        return instanceQueueName;
    }

    public void setInstanceQueueName(String instanceQueueName) {
        this.instanceQueueName = instanceQueueName;
    }

    public String getTaskCompletionToken() {
        return taskCompletionToken;
    }

    public void setTaskCompletionToken(String taskCompletionToken) {
        this.taskCompletionToken = taskCompletionToken;
    }

    public GameMetadata toModel() {
        return new GameMetadata(gameName, fullName, gameReadyState, instanceQueueName, instanceQueueName, taskCompletionToken);
    }

}
