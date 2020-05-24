package io.mamish.serverbot2.appinfra;

import io.mamish.serverbot2.sharedconfig.GameMetadataConfig;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.lambda.Function;

import java.util.List;

public class GameMetadataStack extends Stack {

    public GameMetadataStack(Construct parent, String id) {
        this(parent, id, null);
    }

    public GameMetadataStack(Construct parent, String id, StackProps props) {
        super(parent, id, props);

        Role functionRole = Role.Builder.create(this, "ServiceRole")
                .assumedBy(new ServicePrincipal("lambda.amazonaws.com"))
                .managedPolicies(List.of(
                        Util.POLICY_BASIC_LAMBDA_EXECUTION,
                        Util.POLICY_DYNAMODB_FULL_ACCESS
                )).build();

        Function function = Util.standardJavaFunction(this, "ServiceFunction", "game-metadata-service",
                "io.mamish.serverbot2.gamemetadata.LambdaHandler", functionRole)
                .functionName(GameMetadataConfig.FUNCTION_NAME)
                .build();

    }

}
