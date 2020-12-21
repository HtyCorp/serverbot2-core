package com.admiralbot.networksecurity.firewall;

import com.admiralbot.framework.exception.server.NoSuchResourceException;
import com.admiralbot.framework.exception.server.ResourceAlreadyExistsException;
import com.admiralbot.networksecurity.model.ManagedSecurityGroup;
import com.admiralbot.networksecurity.model.PortPermission;

import java.util.List;
import java.util.Optional;

public interface IGroupManager {

    void createGroup(String name) throws ResourceAlreadyExistsException;
    ManagedSecurityGroup describeGroup(String name) throws NoSuchResourceException;
    List<ManagedSecurityGroup> listGroups();
    void deleteGroup(String name) throws NoSuchResourceException;

    void setUserIp(String userIpAddress, DiscordUserAuthInfo userInfo);
    Optional<DiscordUserAuthInfo> getUserInfoByIp(String userIpAddress);

    void revokeExpiredIps();

    void modifyGroupPorts(ManagedSecurityGroup group, List<PortPermission> ports, boolean addNotRemove);

}
