package io.mamish.serverbot2.networksecurity.securitygroups;

import io.mamish.serverbot2.networksecurity.model.ManagedSecurityGroup;
import io.mamish.serverbot2.networksecurity.model.PortPermission;

import java.util.List;

public interface IGroupManager {

    void createGroup(String name);
    void initialiseBaseGroup();
    void copyBaseRuleIntoGroup(String name);

    ManagedSecurityGroup describeGroup(String name);
    List<ManagedSecurityGroup> listGroups();

    void deleteGroup(String name);

    void addUserToGroup(ManagedSecurityGroup group, String userIpAddress, String userId);
    void removeUserFromGroup(ManagedSecurityGroup group, String userId);

    void modifyPortsInGroup(ManagedSecurityGroup group, List<PortPermission> ports, boolean addNotRemove);
}
