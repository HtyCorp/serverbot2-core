package com.admiralbot.sharedconfig;

/**
 * A deferred configuration value that fetches the latest secret version with the given name/id.
 */
public class Secret extends ConfigValue {

    public Secret(String path, String name) {
        // Secrets have no '/' at the start of their path, unlike SSM parameters
        super(path + "/" + name, MicroAwsClient::secretsManagerGetSecretValue);
    }

}
