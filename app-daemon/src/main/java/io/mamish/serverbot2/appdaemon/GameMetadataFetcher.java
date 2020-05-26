package io.mamish.serverbot2.appdaemon;

import io.mamish.serverbot2.framework.client.ApiClient;
import io.mamish.serverbot2.gamemetadata.model.GameMetadata;
import io.mamish.serverbot2.gamemetadata.model.IGameMetadataService;
import io.mamish.serverbot2.gamemetadata.model.IdentifyInstanceRequest;
import io.mamish.serverbot2.sharedconfig.GameMetadataConfig;

public class GameMetadataFetcher {

    private final IGameMetadataService gameMetadataServiceClient = ApiClient.lambda(IGameMetadataService.class,
            GameMetadataConfig.FUNCTION_NAME);
    private final GameMetadata initialMetadata;

    public GameMetadataFetcher() {
        initialMetadata = fetch();
    }

    public GameMetadata initial() {
        return initialMetadata;
    }

    public GameMetadata fetch() {
        InstanceMetadata instanceMetadata = InstanceMetadata.fetch();
        return gameMetadataServiceClient.identifyInstance(
                new IdentifyInstanceRequest(instanceMetadata.getInstanceId())
        ).getMetadata();
    }

}
