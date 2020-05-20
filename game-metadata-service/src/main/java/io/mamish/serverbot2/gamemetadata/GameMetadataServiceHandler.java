package io.mamish.serverbot2.gamemetadata;

import io.mamish.serverbot2.dynamomapper.EqualsCondition;
import io.mamish.serverbot2.dynamomapper.UpdateSetter;
import io.mamish.serverbot2.framework.exception.server.RequestHandlingException;
import io.mamish.serverbot2.framework.exception.server.RequestValidationException;
import io.mamish.serverbot2.gamemetadata.model.*;
import io.mamish.serverbot2.sharedconfig.GameMetadataConfig;
import io.mamish.serverbot2.dynamomapper.DynamoObjectMapper;
import io.mamish.serverbot2.sharedutil.IDUtils;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.util.List;

public class GameMetadataServiceHandler implements IGameMetadataService {

    SqsClient sqsClient = SqsClient.create();
    private final DynamoObjectMapper<GameMetadata> mapper
            = new DynamoObjectMapper<>(GameMetadataConfig.BACKING_TABLE_NAME, GameMetadata.class);

    @Override
    public ListGamesResponse listGames(ListGamesRequest request) {
        List<GameMetadata> allMetadata = mapper.scan();
        return new ListGamesResponse(allMetadata);
    }

    @Override
    public DescribeGameResponse describeGame(DescribeGameRequest request) {
        GameMetadata result = mapper.get(request.getGameName());
        if (result == null) {
            return new DescribeGameResponse(false, null);
        } else {
            return new DescribeGameResponse(true, result);
        }
    }

    @Override
    public LockGameResponse lockGame(LockGameRequest request) {
        String name = request.getGameName();
        if (!mapper.has(name)) {
            throw new RequestValidationException("No game '" + name + "' present in table");
        }

        // Need to make the mapper more sophisticated so we can do this as one big mapper update call

        try {
            mapper.update(
                    name,
                    new EqualsCondition("launchState", LaunchState.READY),
                    new UpdateSetter("launchState", LaunchState.PREPARING)
            );
        } catch (ConditionalCheckFailedException e) {
            throw new RequestHandlingException("Game state is invalid for locking");
        }

        String tempQueueName = String.join("-",
                "app-daemon-temp", IDUtils.epochSeconds(), IDUtils.randomUUIDJoined());
        String url = sqsClient.createQueue(r -> r.queueName(tempQueueName)).queueUrl();

        mapper.update(name, new UpdateSetter("instanceQueueUrl", url));

        return new LockGameResponse();
    }

}
