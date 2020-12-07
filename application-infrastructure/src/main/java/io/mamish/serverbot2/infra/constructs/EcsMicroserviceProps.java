package io.mamish.serverbot2.infra.constructs;

public class EcsMicroserviceProps {

    private final String javaModuleName;

    public EcsMicroserviceProps(String javaModuleName) {
        this.javaModuleName = javaModuleName;
    }

    public String getJavaModuleName() {
        return javaModuleName;
    }
}
