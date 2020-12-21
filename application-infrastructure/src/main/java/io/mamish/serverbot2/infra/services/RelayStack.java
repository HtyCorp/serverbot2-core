package io.mamish.serverbot2.infra.services;

import io.mamish.serverbot2.commandlambda.model.ICommandService;
import io.mamish.serverbot2.discordrelay.model.service.IDiscordService;
import io.mamish.serverbot2.infra.constructs.EcsMicroservice;
import io.mamish.serverbot2.infra.constructs.ServiceApi;
import io.mamish.serverbot2.infra.deploy.ApplicationStage;
import io.mamish.serverbot2.infra.util.ManagedPolicies;
import io.mamish.serverbot2.infra.util.Permissions;
import io.mamish.serverbot2.sharedconfig.DiscordConfig;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.BillingMode;
import software.amazon.awscdk.services.dynamodb.Table;

public class RelayStack extends Stack {

    public RelayStack(ApplicationStage parent, String id) {
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
        Permissions.addManagedPoliciesToRole(service.getTaskRole(), ManagedPolicies.SQS_FULL_ACCESS);
        messageTable.grantFullAccess(service);

        ServiceApi api = new ServiceApi(this, "Api", parent, IDiscordService.class);
        api.addEcsRoute(IDiscordService.class, service);

    }

}
