package io.mamish.serverbot2.networksecurity.model;

public class PortPermission {

    private PortProtocol protocol;
    private int portRangeFrom;
    private int portRangeTo;

    public PortPermission() { }

    public PortPermission(PortProtocol protocol, int portRangeFrom, int portRangeTo) {
        this.protocol = protocol;
        this.portRangeFrom = portRangeFrom;
        this.portRangeTo = portRangeTo;
    }

    public PortProtocol getProtocol() {
        return protocol;
    }

    public int getPortRangeFrom() {
        return portRangeFrom;
    }

    public int getPortRangeTo() {
        return portRangeTo;
    }
}
