package io.mamish.serverbot2.infra.services;

import io.mamish.serverbot2.infra.util.Permissions;
import io.mamish.serverbot2.infra.util.Util;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedconfig.LambdaWarmerConfig;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.events.Rule;
import software.amazon.awscdk.services.events.Schedule;
import software.amazon.awscdk.services.events.targets.LambdaFunction;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.lambda.Alias;

import java.util.List;

public class LambdaWarmerStack extends Stack {

    public LambdaWarmerStack(Construct parent, String id) {
        super(parent, id, null);

        Role lambdaFunctionRole = Util.standardLambdaRole(this, "WarmerFunctionRole", List.of()).build();

        Permissions.addConfigPathRead(this, lambdaFunctionRole,CommonConfig.PATH);
        Permissions.addLambdaInvoke(this, lambdaFunctionRole,
                LambdaWarmerConfig.FUNCTION_NAMES_TO_WARM.toArray(String[]::new));
        Permissions.addFullExecuteApi(this, lambdaFunctionRole);

        Alias warmerFunctionAlias = Util.highMemJavaFunction(this, "WarmerFunction", "lambda-warmer",
                "io.mamish.serverbot2.lambdawarmer.ScheduledLambdaHandler",
                b -> b.role(lambdaFunctionRole));

        Rule rateRule = Rule.Builder.create(this, "WarmerRateRule")
                .schedule(Schedule.rate(Duration.seconds(LambdaWarmerConfig.WARMER_PING_INTERVAL_SECONDS)))
                .targets(List.of(new LambdaFunction(warmerFunctionAlias)))
                .build();

    }

}
