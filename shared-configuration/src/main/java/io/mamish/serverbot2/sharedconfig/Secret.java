package io.mamish.serverbot2.sharedconfig;

import io.mamish.serverbot2.sharedutil.SdkUtils;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

import java.util.function.Function;

/**
 * A deferred configuration value that fetches the latest secret version with the given name/id.
 */
public class Secret extends ConfigValue {

    private static final SecretsManagerClient secretsManager = SdkUtils.client(SecretsManagerClient.builder());
    private static final Function<String,String> fetcher = n -> secretsManager.getSecretValue(r -> r.secretId(n)).secretString();

    public Secret(String path, String name) {
        // Secrets have no '/' at the start of their path, unlike SSM parameters
        super(path + "/" + name, fetcher);
    }

}
