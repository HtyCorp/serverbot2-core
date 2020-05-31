package io.mamish.serverbot2.networksecurity.model;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PortPermission that = (PortPermission) o;
        return portRangeFrom == that.portRangeFrom &&
                portRangeTo == that.portRangeTo &&
                protocol == that.protocol;
    }

    @Override
    public int hashCode() {
        return Objects.hash(protocol, portRangeFrom, portRangeTo);
    }
}
