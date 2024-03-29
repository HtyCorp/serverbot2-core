package com.admiralbot.appdaemon;

import com.admiralbot.framework.client.ApiClient;
import com.admiralbot.gamemetadata.model.GameMetadata;
import com.admiralbot.gamemetadata.model.IGameMetadataService;
import com.admiralbot.gamemetadata.model.IdentifyInstanceRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GameMetadataFetcher {

    private static final Logger logger = LoggerFactory.getLogger(GameMetadataFetcher.class);

    private static final IGameMetadataService gameMetadataService = ApiClient.http(IGameMetadataService.class);
    private static final GameMetadata initialMetadata = fetch();

    public static GameMetadata initial() {
        return initialMetadata;
    }

    public static GameMetadata fetch() {
        InstanceMetadata instanceMetadata = InstanceMetadata.fetch();
        logger.debug("Identifying instance through GMS");
        return gameMetadataService.identifyInstance(
                new IdentifyInstanceRequest(instanceMetadata.getInstanceId())
        ).getMetadata();
    }

}
