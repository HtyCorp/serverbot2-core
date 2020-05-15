package io.mamish.serverbot2.networksecurity.model;

import io.mamish.serverbot2.framework.common.ApiArgumentInfo;
import io.mamish.serverbot2.framework.common.ApiRequestInfo;

import java.util.List;

@ApiRequestInfo(order = 0, name = "CreateSecurityGroup", numRequiredFields = 3, description = "Create a dynamic security group which updates its rules when users authorize their IP using !addip")
public class CreateSecurityGroupRequest {

    @ApiArgumentInfo(order = 0, name = "gameName", description = "Unique name for group, nominally same as game short name")
    private String gameName;

    @ApiArgumentInfo(order = 1, name = "tcpPorts", description = "List of TCP ports to open to authorised users")
    private List<Integer> tcpPorts;

    @ApiArgumentInfo(order = 2, name = "udpPorts", description = "List of UDP ports to open to authorised users")
    private List<Integer> udpPorts;

    public CreateSecurityGroupRequest() { }

    public CreateSecurityGroupRequest(String gameName, List<Integer> tcpPorts, List<Integer> udpPorts) {
        this.gameName = gameName;
        this.tcpPorts = tcpPorts;
        this.udpPorts = udpPorts;
    }

    public String getGameName() {
        return gameName;
    }

    public List<Integer> getTcpPorts() {
        return tcpPorts;
    }

    public List<Integer> getUdpPorts() {
        return udpPorts;
    }
}
