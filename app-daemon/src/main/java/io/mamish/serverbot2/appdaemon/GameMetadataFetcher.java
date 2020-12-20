package io.mamish.serverbot2.appdaemon;

import io.mamish.serverbot2.framework.client.ApiClient;
import io.mamish.serverbot2.gamemetadata.model.GameMetadata;
import io.mamish.serverbot2.gamemetadata.model.IGameMetadataService;
import io.mamish.serverbot2.gamemetadata.model.IdentifyInstanceRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GameMetadataFetcher {

    private static final Logger logger = LogManager.getLogger(GameMetadataFetcher.class);

    private static final IGameMetadataService gameMetadataService = ApiClient.http(IGameMetadataService.class);
    private static final GameMetadata initialMetadata = fetch();

    public static GameMetadata initial() {
        return initialMetadata;
    }

    public static GameMetadata fetch() {
        logger.debug("Fetch: getting instance metadata");
        InstanceMetadata instanceMetadata = InstanceMetadata.fetch();
        logger.debug("Identifying instance through GMS");
        return gameMetadataService.identifyInstance(
                new IdentifyInstanceRequest(instanceMetadata.getInstanceId())
        ).getMetadata();
    }

}
