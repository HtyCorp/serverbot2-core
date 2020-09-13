package io.mamish.serverbot2.networksecurity.model;

public enum PortProtocol {

    TCP, UDP, ICMP;

    public static String toLowerCaseName(PortProtocol protocol) {
        return protocol.name().toLowerCase();
    }

    public static int toProtocolNumber(PortProtocol protocol) {
        switch(protocol) {
            case TCP:
                return 6;
            case UDP:
                return 17;
            case ICMP:
                return 1;
            default:
                throw new IllegalArgumentException("Not a protocol with a set number: " + protocol.toString());
        }
    }

    public static PortProtocol fromLowerCaseName(String apiName) throws IllegalArgumentException {
        return PortProtocol.valueOf(apiName.toUpperCase());
    }

}
