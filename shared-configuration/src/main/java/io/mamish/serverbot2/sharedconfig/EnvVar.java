package io.mamish.serverbot2.sharedconfig;

public class EnvVar {

    private String name;
    private String value;

    public EnvVar(String name) {
        this.name = name;
        this.value = System.getenv(name);
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}
