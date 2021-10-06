package com.admiralbot.networksecurity.model;

import com.admiralbot.framework.modelling.ApiArgumentInfo;
import com.admiralbot.framework.modelling.ApiRequestInfo;

import java.util.List;

@ApiRequestInfo(order = 2, name = "ModifyPorts", numRequiredFields = 1, description = "Modify ports of a registered security group")
public class ModifyPortsRequest {

    @ApiArgumentInfo(order = 0, description = "Name of group to update")
    private String gameName;

    @ApiArgumentInfo(order = 1, description = "New ports to allow")
    private List<PortPermission> addPorts;

    @ApiArgumentInfo(order = 2, description = "Existing ports to remove")
    private List<PortPermission> removePorts;

    public ModifyPortsRequest() { }

    public ModifyPortsRequest(String gameName, List<PortPermission> addPorts, List<PortPermission> removePorts) {
        this.gameName = gameName;
        this.addPorts = addPorts;
        this.removePorts = removePorts;
    }

    public String getGameName() {
        return gameName;
    }

    public List<PortPermission> getAddPorts() {
        return addPorts;
    }

    public List<PortPermission> getRemovePorts() {
        return removePorts;
    }
}
