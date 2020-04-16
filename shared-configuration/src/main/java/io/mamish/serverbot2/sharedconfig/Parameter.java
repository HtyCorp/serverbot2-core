package io.mamish.serverbot2.sharedconfig;

import software.amazon.awssdk.services.ssm.SsmClient;

public class Parameter {

    private static final SsmClient ssmClient = SsmClient.builder()
            .region(CommonConfig.REGION)
            .build();

    private String name;
    private String value;

    public Parameter(String name) {
        this.name = name;
        this.value = getParameterString(name);
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    private static String getParameterString(String parameterName) {
        return ssmClient.getParameter(r -> r.name(parameterName)).parameter().value();
    }



}
