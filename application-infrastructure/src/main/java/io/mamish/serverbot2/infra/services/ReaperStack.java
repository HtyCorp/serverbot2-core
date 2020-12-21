package io.mamish.serverbot2.infra.services;

import io.mamish.serverbot2.infra.util.ManagedPolicies;
import io.mamish.serverbot2.infra.util.Permissions;
import io.mamish.serverbot2.infra.util.Util;
import io.mamish.serverbot2.networksecurity.model.INetworkSecurity;
import io.mamish.serverbot2.sharedconfig.ReaperConfig;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.events.Rule;
import software.amazon.awscdk.services.events.Schedule;
import software.amazon.awscdk.services.events.targets.LambdaFunction;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.lambda.Alias;

import java.util.List;

public class ReaperStack extends Stack {

    public ReaperStack(Construct parent, String id) {
        super(parent, id);

        Role functionRole = Util.standardLambdaRole(this, "ReaperFunctionRole", List.of(
                ManagedPolicies.SQS_FULL_ACCESS
        )).build();

        // NetSec permission required to invoke RevokeExpiredIps
        Permissions.addExecuteApi(this, functionRole, INetworkSecurity.class);

        Alias scheduledFunctionAlias = Util.highMemJavaFunction(this, "ReaperFunction", "resource-reaper",
                "io.mamish.serverbot2.resourcereaper.ScheduledLambdaHandler",
                b -> b.role(functionRole));

        Rule rateRule = Rule.Builder.create(this, "ReaperScheduleRule")
                .schedule(Schedule.rate(Duration.seconds(ReaperConfig.EXECUTION_INTERVAL_SECONDS)))
                .targets(List.of(new LambdaFunction(scheduledFunctionAlias)))
                .build();

    }

}
