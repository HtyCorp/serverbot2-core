package io.mamish.serverbot2.appdaemon;

import io.mamish.serverbot2.framework.client.ApiClient;
import io.mamish.serverbot2.gamemetadata.model.GameMetadata;
import io.mamish.serverbot2.gamemetadata.model.IGameMetadataService;
import io.mamish.serverbot2.gamemetadata.model.IdentifyInstanceRequest;
import io.mamish.serverbot2.sharedconfig.GameMetadataConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GameMetadataFetcher {

    private static final Logger logger = LogManager.getLogger(GameMetadataFetcher.class);

    private static final IGameMetadataService gameMetadataServiceClient = ApiClient.lambda(IGameMetadataService.class,
            GameMetadataConfig.FUNCTION_NAME);
    private static final GameMetadata cachedMetadata = fetch();

    public static GameMetadata cached() {
        return cachedMetadata;
    }

    public static GameMetadata fetch() {
        logger.debug("Fetch: getting instance metadata");
        InstanceMetadata instanceMetadata = InstanceMetadata.fetch();
        logger.debug("Identifying instance through GMS");
        return gameMetadataServiceClient.identifyInstance(
                new IdentifyInstanceRequest(instanceMetadata.getInstanceId())
        ).getMetadata();
    }

}
