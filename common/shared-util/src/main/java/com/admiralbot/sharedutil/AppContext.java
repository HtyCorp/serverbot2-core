package com.admiralbot.sharedutil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.regions.providers.InstanceProfileRegionProvider;

import java.util.Objects;
import java.util.function.Supplier;

public class AppContext {

    private static final String FORCE_DEFAULT_ENV_VAR_NAME = "APP_CONTEXT_FORCE_DEFAULT";

    private static final Logger logger = LoggerFactory.getLogger(AppContext.class);

    private static AppContext globalContext;

    private final Region region;
    private final AwsCredentialsProvider credentialsProvider;
    private final SdkHttpClient httpClient;

    public static AppContext get() {
        return Objects.requireNonNull(globalContext, "AppContext not initialised");
    }

    public static AppContext getAsGlobalRegion() {
        return new AppContext(
                Region.AWS_GLOBAL,
                globalContext.getCredentialsProvider(),
                globalContext.getHttpClient()
        );
    }

    public static void setLambda() {
        setGlobalContextIfUnset(AppContext::lambdaContext, true);
    }

    public static void setContainer() {
        setGlobalContextIfUnset(AppContext::containerContext, true);
    }

    public static void setInstance() {
        setGlobalContextIfUnset(AppContext::instanceContext, true);
    }

    public static void setDev() {
        setGlobalContextIfUnset(AppContext::devContext, false);
    }

    public static void setDefault() {
        setGlobalContextIfUnset(AppContext::defaultContext, false);
    }

    private static void setGlobalContextIfUnset(Supplier<AppContext> context, boolean disregardAlreadySetError) {
        if (globalContext == null) {
            logger.info("Set new global app context");
            globalContext = "true".equals(System.getenv(FORCE_DEFAULT_ENV_VAR_NAME))
                    ? defaultContext()
                    : context.get();
        } else if (disregardAlreadySetError) {
            logger.info("Ignoring requested global app context, since one is already set");
        } else {
            throw new IllegalStateException("A global app context is already set");
        }
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

    private static AppContext devContext() {
        return new AppContext(
                Region.AP_SOUTHEAST_2,
                ProfileCredentialsProvider.create("devfrozen"),
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
