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

public class MockGroupManager implements IGroupManager {

    private Map<String,ManagedSecurityGroup> store = new HashMap<>();
    private final Crypto crypto;

    public MockGroupManager(Crypto crypto) {
        this.crypto = crypto;
    }

    @Override
    public void createGroup(String name) {
        ManagedSecurityGroup newGroup = new ManagedSecurityGroup(name, IDUtils.randomUUIDJoined(),
                crypto.generateDataKey().snd());
        store.put(newGroup.getName(), newGroup);
    }

    @Override
    public void initialiseBaseGroup() {
        updateGroup(NetSecConfig.REFERENCE_SG_NAME, group -> {

            Key dataKey = crypto.decryptDataKey(group.getEncryptedDataKey());

            List<PortPermission> newPorts = new ArrayList<>(group.getAllowedPorts());
            newPorts.add(new PortPermission(PortProtocol.ICMP, -1, -1));

            String encryptedUserId = crypto.encryptLocal(SdkBytes.fromUtf8String("REFERENCE_DO_NOT_DELETE"), dataKey);
            List<DiscordUserIp> newUsers = List.of(new DiscordUserIp(encryptedUserId, "0.0.0.0"));

            return new Pair<>(newPorts, newUsers);

        });
    }

    @Override
    public void copyBaseRuleIntoGroup(String name) {
        ManagedSecurityGroup ref = findGroup(NetSecConfig.REFERENCE_SG_NAME);
        // Similar to Ec2GroupManager, lift the one and only port permission from ref, copying its source addresses
        updateGroup(name, _g -> {
            return new Pair<>(ref.getAllowedPorts(), ref.getAllowedUsers());
        });
    }

    @Override
    public ManagedSecurityGroup describeGroup(String name) {
        return findGroup(name);
    }

    @Override
    public List<ManagedSecurityGroup> listGroups() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void deleteGroup(String name) {
        findGroup(name); // to throw exception if missing
        store.remove(name);
    }

    @Override
    public void modifyUserInGroup(ManagedSecurityGroup inputGroup, String userAddress, String userId, boolean addNotRemove) {
        updateGroup(inputGroup, group -> {
            List<DiscordUserIp> newUsers = new ArrayList<>(group.getAllowedUsers());
            Key dataKey = crypto.decryptDataKey(group.getEncryptedDataKey());

            if (addNotRemove) {
                String encryptedUserId = crypto.encryptLocal(SdkBytes.fromUtf8String(userId), dataKey);
                DiscordUserIp newUser = new DiscordUserIp(encryptedUserId, userAddress);
                newUsers.add(newUser);
            } else {
                newUsers.removeIf(u -> crypto.decryptLocal(u.getDiscordId(), dataKey).asUtf8String().equals(userId));
            }

            return new Pair<>(null, newUsers);
        });
    }

    @Override
    public void modifyPortsInGroup(ManagedSecurityGroup inputGroup, List<PortPermission> ports, boolean addNotRemove) {
        updateGroup(inputGroup, group -> {
            List<PortPermission> newPorts = new ArrayList<>(group.getAllowedPorts());
            if (addNotRemove) {
                newPorts.addAll(ports);
            } else {
                newPorts.removeAll(ports);
            }
            return new Pair<>(newPorts, null);
        });
    }

    private ManagedSecurityGroup findGroup(String name) {
        ManagedSecurityGroup group = store.get(name);
        if (group == null) {
            throw new NoSuchResourceException("No such group added to local store");
        }
        return group;
    }

    private void updateGroup(String name, Function<ManagedSecurityGroup, Pair<List<PortPermission>,List<DiscordUserIp>>> updater) {
        ManagedSecurityGroup group = findGroup(name);
        updateGroup(group, updater);
    }

    private void updateGroup(ManagedSecurityGroup group, Function<ManagedSecurityGroup, Pair<List<PortPermission>,List<DiscordUserIp>>> updater) {
        Pair<List<PortPermission>,List<DiscordUserIp>> newData = updater.apply(group);
        List<PortPermission> ports = newData.fst();
        List<DiscordUserIp> users = newData.snd();
        ManagedSecurityGroup updated = new ManagedSecurityGroup(
                group.getName(),
                group.getGroupId(),
                group.getEncryptedDataKey(),
                (ports != null) ? ports : group.getAllowedPorts(),
                (users != null) ? users : group.getAllowedUsers()
        );
        store.put(updated.getName(), updated);
    }

}
