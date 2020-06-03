package io.mamish.serverbot2.networksecurity.securitygroups;

import io.mamish.serverbot2.framework.exception.server.NoSuchResourceException;
import io.mamish.serverbot2.networksecurity.crypto.Crypto;
import io.mamish.serverbot2.networksecurity.model.DiscordUserIp;
import io.mamish.serverbot2.networksecurity.model.ManagedSecurityGroup;
import io.mamish.serverbot2.networksecurity.model.PortPermission;
import io.mamish.serverbot2.networksecurity.model.PortProtocol;
import io.mamish.serverbot2.sharedconfig.NetSecConfig;
import io.mamish.serverbot2.sharedutil.IDUtils;
import io.mamish.serverbot2.sharedutil.Pair;
import software.amazon.awssdk.core.SdkBytes;

import java.security.Key;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MockGroupManager implements IGroupManager {

    private final Map<String,ManagedSecurityGroup> store = new HashMap<>();
    private final Crypto crypto;

    public MockGroupManager(Crypto crypto) {
        this.crypto = crypto;
    }

    @Override
    public void createGroup(String name) {
        ManagedSecurityGroup newGroup = new ManagedSecurityGroup(name, IDUtils.randomUUIDJoined(),
                crypto.generateDataKey().b());
        store.put(newGroup.getName(), newGroup);
    }

    @Override
    public void initialiseBaseGroup() {
        updateGroupInStore(NetSecConfig.REFERENCE_SG_NAME, group -> {

            if (group.getAllowedPorts() != null) {
                throw new IllegalStateException("Ports already initialised somehow");
            }

            List<PortPermission> newPorts = List.of(
                    new PortPermission(PortProtocol.ICMP, -1, -1));
            List<DiscordUserIp> newUsers = List.of(
                    new DiscordUserIp("REFERENCE_DO_NOT_DELETE", "0.0.0.0"));

            return new Pair<>(newPorts, newUsers);

        });
    }

    @Override
    public void copyBaseRuleIntoGroup(String name) {
        ManagedSecurityGroup ref = withDecryptedUsers(getEncryptedGroup(NetSecConfig.REFERENCE_SG_NAME));
        // Similar to Ec2GroupManager, lift the one and only port permission from ref, copying its source addresses
        updateGroupInStore(name, group -> new Pair<>(ref.getAllowedPorts(), ref.getAllowedUsers()));
    }

    @Override
    public ManagedSecurityGroup describeGroup(String name) {
        return withDecryptedUsers(getEncryptedGroup(name));
    }

    @Override
    public List<ManagedSecurityGroup> listGroups() {
        return store.values().stream().map(this::withDecryptedUsers).collect(Collectors.toList());
    }

    @Override
    public void deleteGroup(String name) {
        getEncryptedGroup(name); // to throw exception if missing
        store.remove(name);
    }

    @Override
    public void addUserToGroup(ManagedSecurityGroup inputGroup, String userIpAddress, String userId) {
        updateGroupInStore(inputGroup, group -> {
            List<DiscordUserIp> newUsers = new ArrayList<>(group.getAllowedUsers());
            newUsers.add(new DiscordUserIp(userId, userIpAddress));
            return new Pair<>(null, newUsers);
        });
    }

    @Override
    public void removeUserFromGroup(ManagedSecurityGroup inputGroup, String userId) {
        updateGroupInStore(inputGroup, group -> {
            List<DiscordUserIp> newUsers = new ArrayList<>(group.getAllowedUsers());
            newUsers.removeIf(u -> u.getDiscordId().equals(userId));
            return new Pair<>(null, newUsers);
        });
    }

    @Override
    public void modifyPortsInGroup(ManagedSecurityGroup inputGroup, List<PortPermission> ports, boolean addNotRemove) {
        updateGroupInStore(inputGroup, group -> {
            List<PortPermission> newPorts = new ArrayList<>(group.getAllowedPorts());
            if (addNotRemove) {
                newPorts.addAll(ports);
            } else {
                newPorts.removeAll(ports);
            }
            return new Pair<>(newPorts, null);
        });
    }

    private ManagedSecurityGroup getEncryptedGroup(String name) {
        ManagedSecurityGroup group = store.get(name);
        if (group == null) {
            throw new NoSuchResourceException("No such group added to local store");
        }
        return group;
    }

    private ManagedSecurityGroup withDecryptedUsers(ManagedSecurityGroup group) {
        if (group.getAllowedUsers() == null) {
            return group;
        }
        return withPortsAndUsers(group, null, group.getAllowedUsers().stream().map(u -> {
            Key dataKey = crypto.decryptDataKey(group.getEncryptedDataKey());
            String decryptedId = crypto.decryptLocal(u.getDiscordId(), dataKey).asUtf8String();
            return new DiscordUserIp(decryptedId, u.getIpAddress());
        }).collect(Collectors.toList()));
    }

    private ManagedSecurityGroup withEncryptedUsers(ManagedSecurityGroup group) {
        if (group.getAllowedUsers() == null) {
            return group;
        }
        return withPortsAndUsers(group, null, group.getAllowedUsers().stream().map(u -> {
            Key dataKey = crypto.decryptDataKey(group.getEncryptedDataKey());
            String encryptedId = crypto.encryptLocal(SdkBytes.fromUtf8String(u.getDiscordId()), dataKey);
            return new DiscordUserIp(encryptedId, u.getIpAddress());
        }).collect(Collectors.toList()));
    }

    private ManagedSecurityGroup withPortsAndUsers(ManagedSecurityGroup group, List<PortPermission> ports, List<DiscordUserIp> users) {
        return new ManagedSecurityGroup(
                group.getName(),
                group.getGroupId(),
                group.getEncryptedDataKey(),
                (ports != null) ? ports : group.getAllowedPorts(),
                (users != null) ? users : group.getAllowedUsers()
        );
    }

    private void updateGroupInStore(String name, Function<ManagedSecurityGroup, Pair<List<PortPermission>,List<DiscordUserIp>>> updater) {
        ManagedSecurityGroup group = withDecryptedUsers(getEncryptedGroup(name));
        updateGroupInStore(group, updater);
    }

    private void updateGroupInStore(ManagedSecurityGroup group, Function<ManagedSecurityGroup, Pair<List<PortPermission>,List<DiscordUserIp>>> updater) {
        Pair<List<PortPermission>,List<DiscordUserIp>> newData = updater.apply(group);
        List<PortPermission> ports = newData.a();
        List<DiscordUserIp> users = newData.b();
        ManagedSecurityGroup updated = withPortsAndUsers(group, ports, users);
        ManagedSecurityGroup encrypted = withEncryptedUsers(updated);
        store.put(encrypted.getName(), encrypted);
    }

}
