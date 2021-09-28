package com.admiralbot.infra.services;

import com.admiralbot.commandservice.model.ICommandService;
import com.admiralbot.discordrelay.model.service.IDiscordService;
import com.admiralbot.infra.constructs.EcsMicroservice;
import com.admiralbot.infra.constructs.ServiceApi;
import com.admiralbot.infra.deploy.ApplicationRegionalStage;
import com.admiralbot.infra.util.ManagedPolicies;
import com.admiralbot.infra.util.Permissions;
import com.admiralbot.sharedconfig.DiscordConfig;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.BillingMode;
import software.amazon.awscdk.services.dynamodb.Table;

public class RelayStack extends Stack {

    public RelayStack(ApplicationRegionalStage parent, String id) {
        super(parent, id);

        // Data stores for relay

        Attribute messageTablePartitionKey = Attribute.builder()
                .name(DiscordConfig.MESSAGE_TABLE_PKEY)
                .type(AttributeType.STRING)
                .build();
        Table messageTable = Table.Builder.create(this, "DiscordRelayMessageTable")
                .tableName(DiscordConfig.MESSAGE_TABLE_NAME)
                .removalPolicy(RemovalPolicy.DESTROY)
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .partitionKey(messageTablePartitionKey)
                .build();

        EcsMicroservice service = new EcsMicroservice(this, "EcsMicroservice", parent, "discord-relay");
        Permissions.addConfigPathRead(this, service, DiscordConfig.PATH_ALL);
        Permissions.addExecuteApi(this, service, ICommandService.class);
        Permissions.addManagedPoliciesToRole(service.getRole(), ManagedPolicies.SQS_FULL_ACCESS);
        messageTable.grantFullAccess(service);

        ServiceApi api = new ServiceApi(this, "Api", parent, IDiscordService.class);
        api.addEcsRoute(IDiscordService.class, service);

    }

}
