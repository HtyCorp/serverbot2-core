package io.mamish.serverbot2.appinfra;

import io.mamish.serverbot2.sharedconfig.GameMetadataConfig;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.dynamodb.*;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.lambda.Function;

import java.util.List;

public class GameMetadataStack extends Stack {

    public GameMetadataStack(Construct parent, String id) {
        this(parent, id, null);
    }

    public GameMetadataStack(Construct parent, String id, StackProps props) {
        super(parent, id, props);

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
                .partitionKey(gameNameAttribute)
                .build();

        metadataTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
                .indexName(GameMetadataConfig.NAME_INSTANCE_ID_INDEX)
                .partitionKey(instanceIdAttribute)
                .projectionType(ProjectionType.ALL)
                .build());

        // Function and attached role

        Role functionRole = Util.standardLambdaRole(this, "ServiceRole", List.of(
                Util.POLICY_BASIC_LAMBDA_EXECUTION,
                Util.POLICY_DYNAMODB_FULL_ACCESS
        )).build();

        Function function = Util.standardJavaFunction(this, "ServiceFunction", "game-metadata-service",
                "io.mamish.serverbot2.gamemetadata.LambdaHandler", functionRole)
                .functionName(GameMetadataConfig.FUNCTION_NAME)
                .build();

    }

}
