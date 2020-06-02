package io.mamish.serverbot2.networksecurity.model;

public enum PortProtocol {

    TCP, UDP, ICMP;

    public static String toLowerCaseName(PortProtocol protocol) {
        return protocol.name().toLowerCase();
    }

    public static PortProtocol fromLowerCaseName(String apiName) throws IllegalArgumentException {
        return PortProtocol.valueOf(apiName.toUpperCase());
    }

}
