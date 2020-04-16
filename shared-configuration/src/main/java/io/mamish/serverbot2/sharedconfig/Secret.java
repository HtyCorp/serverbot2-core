package io.mamish.serverbot2.sharedconfig;

import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

public class Secret {

    private static final SecretsManagerClient secretsManager = SecretsManagerClient.builder()
            .region(CommonConfig.REGION)
            .build();

    private String name;
    private String value;

    public Secret(String name) {
        this.name = name;
        this.value = getSecretString(name);
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    static String getSecretString(String secretId) {
        return secretsManager.getSecretValue(r -> r.secretId(secretId)).secretString();
    }

}
