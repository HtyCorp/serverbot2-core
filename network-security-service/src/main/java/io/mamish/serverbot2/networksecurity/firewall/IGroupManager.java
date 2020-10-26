package io.mamish.serverbot2.networksecurity.firewall;

import io.mamish.serverbot2.framework.exception.server.NoSuchResourceException;
import io.mamish.serverbot2.framework.exception.server.ResourceAlreadyExistsException;
import io.mamish.serverbot2.networksecurity.model.ManagedSecurityGroup;
import io.mamish.serverbot2.networksecurity.model.PortPermission;

import java.util.List;

public interface IGroupManager {

    void createGroup(String name) throws ResourceAlreadyExistsException;
    ManagedSecurityGroup describeGroup(String name) throws NoSuchResourceException;
    List<ManagedSecurityGroup> listGroups();
    void deleteGroup(String name) throws NoSuchResourceException;

    void setUserIp(String userIpAddress, DiscordUserAuthInfo userInfo);

    void modifyGroupPorts(ManagedSecurityGroup group, List<PortPermission> ports, boolean addNotRemove);
}
