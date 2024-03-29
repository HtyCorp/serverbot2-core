package com.admiralbot.infra.services;

import com.admiralbot.infra.constructs.NativeLambdaMicroservice;
import com.admiralbot.infra.constructs.ServiceApi;
import com.admiralbot.infra.deploy.ApplicationRegionalStage;
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

        // Add standard microservice and API

        NativeLambdaMicroservice service = new NativeLambdaMicroservice(this, "Service", parent,
                "url-shortener-service");
        urlTable.grantFullAccess(service.getRole());

        ServiceApi api = new ServiceApi(this, "Api", parent, IUrlShortener.class);
        api.addNativeLambdaRoute(IUrlShortener.class, service);

    }

}
