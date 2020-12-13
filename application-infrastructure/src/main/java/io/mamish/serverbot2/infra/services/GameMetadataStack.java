package io.mamish.serverbot2.infra.services;

import io.mamish.serverbot2.gamemetadata.model.IGameMetadataService;
import io.mamish.serverbot2.infra.constructs.EcsMicroservice;
import io.mamish.serverbot2.infra.constructs.ServiceApi;
import io.mamish.serverbot2.infra.deploy.ApplicationStage;
import io.mamish.serverbot2.infra.util.ManagedPolicies;
import io.mamish.serverbot2.infra.util.Util;
import io.mamish.serverbot2.sharedconfig.GameMetadataConfig;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.dynamodb.*;
import software.amazon.awscdk.services.iam.Role;

import java.util.List;

public class GameMetadataStack extends Stack {

    public GameMetadataStack(ApplicationStage parent, String id) {
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
