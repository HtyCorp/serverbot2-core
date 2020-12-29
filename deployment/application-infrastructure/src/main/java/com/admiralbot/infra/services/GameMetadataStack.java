package com.admiralbot.infra.services;

import com.admiralbot.gamemetadata.model.IGameMetadataService;
import com.admiralbot.infra.constructs.EcsMicroservice;
import com.admiralbot.infra.constructs.ServiceApi;
import com.admiralbot.infra.deploy.ApplicationRegionalStage;
import com.admiralbot.sharedconfig.GameMetadataConfig;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.dynamodb.*;

public class GameMetadataStack extends Stack {

    public GameMetadataStack(ApplicationRegionalStage parent, String id) {
        super(parent, id);

        // DDB table

        Attribute gameNameAttribute = Attribute.builder()
                .type(AttributeType.STRING)
                .name(GameMetadataConfig.PARTITION_KEY_MAIN)
                .build();
        Attribute instanceIdAttribute = Attribute.builder()
                .type(AttributeType.STRING)
                .name(GameMetadataConfig.PARTITION_KEY_INSTANCE_ID_INDEX)
                .build();

        Table metadataTable = Table.Builder.create(this, "GameMetadataTable")
                .tableName(GameMetadataConfig.TABLE_NAME)
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .removalPolicy(RemovalPolicy.DESTROY)
                .partitionKey(gameNameAttribute)
                .build();

        metadataTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
                .indexName(GameMetadataConfig.NAME_INSTANCE_ID_INDEX)
                .partitionKey(instanceIdAttribute)
                .projectionType(ProjectionType.ALL)
                .build());

        EcsMicroservice service = new EcsMicroservice(this, "Service", parent, "game-metadata-service");
        metadataTable.grantFullAccess(service);

        ServiceApi api = new ServiceApi(this, "Api", parent, IGameMetadataService.class);
        api.addEcsRoute(IGameMetadataService.class, service);

    }

}
