package io.mamish.serverbot2.infra.core;

import io.mamish.serverbot2.infra.util.Util;
import io.mamish.serverbot2.sharedconfig.ReaperConfig;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.events.Rule;
import software.amazon.awscdk.services.events.Schedule;
import software.amazon.awscdk.services.events.targets.LambdaFunction;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.lambda.Function;

import java.util.List;

public class ReaperStack extends Stack {

    public ReaperStack(Construct parent, String id, StackProps props) {
        super(parent, id, props);

        Role functionRole = Util.standardLambdaRole(this, "ReaperFunctionRole", List.of(
                Util.POLICY_SQS_FULL_ACCESS
        )).build();

        Function scheduledFunction = Util.standardJavaFunction(this, "ReaperFunction", "resource-reaper",
                "io.mamish.serverbot2.resourcereaper.ScheduledLambdaHandler", functionRole)
                .build();

        Rule rateRule = Rule.Builder.create(this, "ReaperScheduleRule")
                .schedule(Schedule.rate(Duration.seconds(ReaperConfig.EXECUTION_INTERVAL)))
                .build();

        rateRule.addTarget(new LambdaFunction(scheduledFunction));

    }

}
