package io.mamish.serverbot2.sharedutil;

import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.regions.providers.InstanceProfileRegionProvider;

public class AppContext {

    private static AppContext globalContext;

    private final Region region;
    private final AwsCredentialsProvider credentialsProvider;
    private final SdkHttpClient httpClient;

    public static AppContext get() {
        return globalContext;
    }

    public static AppContext getAsGlobalRegion() {
        return new AppContext(
                Region.AWS_GLOBAL,
                globalContext.getCredentialsProvider(),
                globalContext.getHttpClient()
        );
    }

    public static void setLambda() {
        globalContext = lambdaContext();
    }

    public static void setContainer() {
        globalContext = containerContext();
    }

    public static void setInstance() {
        globalContext = instanceContext();
    }

    public static void setDefault() {
        globalContext = defaultContext();
    }

    private static AppContext lambdaContext() {
        return new AppContext(
                Region.of(System.getenv("AWS_REGION")),
                EnvironmentVariableCredentialsProvider.create(),
                UrlConnectionHttpClient.create()
        );
    }

    private static AppContext containerContext() {
        return new AppContext(
                // SDK uses EC2 IMDS by default, which does still work for ECS tasks/containers.
                // Would be nice to add a custom provider to fetch from ECS metadata file or endpoint.
                new InstanceProfileRegionProvider().getRegion(),
                ContainerCredentialsProvider.builder().build(),
                UrlConnectionHttpClient.create()
        );
    }

    private static AppContext instanceContext() {
        return new AppContext(
                new InstanceProfileRegionProvider().getRegion(),
                InstanceProfileCredentialsProvider.create(),
                UrlConnectionHttpClient.create()
        );
    }

    private static AppContext defaultContext() {
        return new AppContext(
                new DefaultAwsRegionProviderChain().getRegion(),
                DefaultCredentialsProvider.create(),
                UrlConnectionHttpClient.create()
        );
    }

    public AppContext(Region region, AwsCredentialsProvider credentialsProvider, SdkHttpClient httpClient) {
        this.region = region;
        this.credentialsProvider = credentialsProvider;
        this.httpClient = httpClient;
    }

    public Region getRegion() {
        return region;
    }

    public AwsCredentialsProvider getCredentialsProvider() {
        return credentialsProvider;
    }

    public SdkHttpClient getHttpClient() {
        return httpClient;
    }

    public AwsCredentials resolveCredentials() {
        return credentialsProvider.resolveCredentials();
    }

}
