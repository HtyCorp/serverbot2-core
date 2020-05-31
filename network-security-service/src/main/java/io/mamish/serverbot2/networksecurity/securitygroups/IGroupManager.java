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

    void modifyUserInGroup(ManagedSecurityGroup group, String userAddress, String userId, boolean addNotRemove);
    void modifyPortsInGroup(ManagedSecurityGroup group, List<PortPermission> ports, boolean addNotRemove);
}
