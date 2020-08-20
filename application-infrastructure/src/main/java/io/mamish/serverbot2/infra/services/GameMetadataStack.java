package io.mamish.serverbot2.infra.services;

import io.mamish.serverbot2.infra.util.Policies;
import io.mamish.serverbot2.infra.util.Util;
import io.mamish.serverbot2.sharedconfig.GameMetadataConfig;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.dynamodb.*;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.lambda.Function;

import java.util.List;

public class GameMetadataStack extends Stack {

    public GameMetadataStack(Construct parent, String id) {
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

        // Function and attached role

        Role functionRole = Util.standardLambdaRole(this, "ServiceRole", List.of(
                Policies.DYNAMODB_FULL_ACCESS
        )).build();

        Function function = Util.standardJavaFunction(this, "ServiceFunction", "game-metadata-service",
                "io.mamish.serverbot2.gamemetadata.LambdaHandler", functionRole)
                .functionName(GameMetadataConfig.FUNCTION_NAME)
                .build();

    }

}
