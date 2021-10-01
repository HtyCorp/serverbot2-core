package com.admiralbot.sharedconfig;

/**
 * A deferred configuration value that fetches the latest SSM parameter with the given name.
 */
public class Parameter extends ConfigValue {

    public Parameter(String name) {
        // SSM params have a '/' at the start of their path, unlike SM secrets
        super("/" + name, MicroAwsClient::ssmGetParameter);
    }

    public Parameter(String path, String name) {
        // SSM params have a '/' at the start of their path, unlike SM secrets
        super("/" + path + "/" + name, MicroAwsClient::ssmGetParameter);
    }

}
