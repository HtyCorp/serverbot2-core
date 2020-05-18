package io.mamish.serverbot2.networksecurity.model;

public enum PortProtocol {

    TCP, UDP;

    public static String toEc2ApiName(PortProtocol protocol) {
        return protocol.name().toLowerCase();
    }

    public static PortProtocol fromEc2ApiName(String apiName) {
        return PortProtocol.valueOf(apiName.toUpperCase());
    }

}
