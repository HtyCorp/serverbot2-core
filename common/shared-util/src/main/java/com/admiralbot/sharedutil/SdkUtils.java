package com.admiralbot.sharedutil;

import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;

import java.util.function.Consumer;

public class SdkUtils {

    private SdkUtils() {}

    public static <T extends AwsClientBuilder<T, U> & AwsSyncClientBuilder<T, U>, U> U client(T builder) {
        return client(builder, AppContext.get(), null);
    }

    public static <T extends AwsClientBuilder<T, U> & AwsSyncClientBuilder<T, U>, U> U client(T builder,
                Consumer<ClientOverrideConfiguration.Builder> overrideBuilder) {
        return client(builder, AppContext.get(), overrideBuilder);
    }

    public static <T extends AwsClientBuilder<T, U> & AwsSyncClientBuilder<T, U>, U> U globalClient(T builder) {
        return client(builder, AppContext.getAsGlobalRegion(), null);
    }

    public static <T extends AwsClientBuilder<T, U> & AwsSyncClientBuilder<T, U>, U> U client(T builder, AppContext context,
            Consumer<ClientOverrideConfiguration.Builder> override) {
        if (context == null) {
            throw new IllegalArgumentException("Couldn't build client since context is null");
        }
        T finalBuilder = builder.credentialsProvider(context.getCredentialsProvider())
                .region(context.getRegion())
                .httpClient(context.getHttpClient());
        if (override != null) {
            finalBuilder.overrideConfiguration(override);
        }
        return finalBuilder.build();
    }

}
