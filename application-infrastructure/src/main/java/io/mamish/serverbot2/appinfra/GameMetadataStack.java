package io.mamish.serverbot2.appinfra;

import io.mamish.serverbot2.sharedconfig.GameMetadataConfig;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;

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
                        ManagedPolicy.fromAwsManagedPolicyName("AWSLambdaBasicExecutionRole"),
                        ManagedPolicy.fromAwsManagedPolicyName("AmazonDynamoDBFullAccess"))
                ).build();

        Function function = Function.Builder.create(this, "ServiceFunction")
                .runtime(Runtime.JAVA_11)
                .functionName(GameMetadataConfig.FUNCTION_NAME)
                .role(functionRole)
                .code(Util.mavenJarAsset("game-metadata-service"))
                .handler("io.mamish.serverbot2.gamemetadata.LambdaHandler")
                .build();

    }

}
