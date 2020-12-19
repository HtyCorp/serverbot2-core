package io.mamish.serverbot2.sharedutil;

import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder;

public class SdkUtils {

    private SdkUtils() {}

    public static <T extends AwsClientBuilder<T, U> & AwsSyncClientBuilder<T, U>, U> U client(T builder) {
        return client(builder, AppContext.get());
    }

    public static <T extends AwsClientBuilder<T, U> & AwsSyncClientBuilder<T, U>, U> U globalClient(T builder) {
        return client(builder, AppContext.getAsGlobalRegion());
    }

    public static <T extends AwsClientBuilder<T, U> & AwsSyncClientBuilder<T, U>, U> U client(T builder, AppContext context) {
        if (context == null) {
            throw new IllegalArgumentException("Couldn't build client since context is null");
        }
        return builder.credentialsProvider(context.getCredentialsProvider())
                .region(context.getRegion())
                .httpClient(context.getHttpClient())
                .build();
    }

}
