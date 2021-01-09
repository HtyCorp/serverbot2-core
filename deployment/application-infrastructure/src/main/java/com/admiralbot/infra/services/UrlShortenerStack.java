package com.admiralbot.infra.services;

import com.admiralbot.discordrelay.model.service.IDiscordService;
import com.admiralbot.infra.constructs.EcsMicroservice;
import com.admiralbot.infra.constructs.ServiceApi;
import com.admiralbot.infra.deploy.ApplicationRegionalStage;
import com.admiralbot.infra.util.Permissions;
import com.admiralbot.sharedconfig.UrlShortenerConfig;
import com.admiralbot.urlshortener.model.IUrlShortener;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.BillingMode;
import software.amazon.awscdk.services.dynamodb.Table;

public class UrlShortenerStack extends Stack {

    public UrlShortenerStack(ApplicationRegionalStage parent, String id) {
        super(parent, id);

        // Configure the DDB table to store URL information

        Attribute partitionKey = Attribute.builder()
                .name(UrlShortenerConfig.TABLE_PARTITION_KEY)
                .type(AttributeType.STRING)
                .build();
        Attribute sortKey = Attribute.builder()
                .name(UrlShortenerConfig.TABLE_SORT_KEY)
                .type(AttributeType.NUMBER)
                .build();
        Table urlTable = Table.Builder.create(this, "UrlInfoTable")
                .tableName(UrlShortenerConfig.DYNAMO_TABLE_NAME)
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .removalPolicy(RemovalPolicy.DESTROY)
                .partitionKey(partitionKey)
                .sortKey(sortKey)
                .build();

        // Configure the DDB table to store user delivery preferences

        Attribute prefsPartitionKey = Attribute.builder()
                .name(UrlShortenerConfig.PREFERENCES_TABLE_PARTITION_KEY)
                .type(AttributeType.STRING)
                .build();
        Table prefsTable = Table.Builder.create(this, "PreferencesTable")
                .tableName(UrlShortenerConfig.DYNAMO_TABLE_NAME)
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        // Add standard microservice and API

        EcsMicroservice microservice = new EcsMicroservice(this, "Service", parent, "url-shortener-service");
        urlTable.grantFullAccess(microservice);
        prefsTable.grantFullAccess(microservice);

        Permissions.addExecuteApi(this, microservice, IDiscordService.class);

        ServiceApi api = new ServiceApi(this, "Api", parent, IUrlShortener.class);
        api.addEcsRoute(IUrlShortener.class, microservice);

    }

}
