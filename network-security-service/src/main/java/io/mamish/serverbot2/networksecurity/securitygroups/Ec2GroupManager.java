package io.mamish.serverbot2.networksecurity.securitygroups;

import io.mamish.serverbot2.framework.exception.server.NoSuchResourceException;
import io.mamish.serverbot2.networksecurity.crypto.Crypto;
import io.mamish.serverbot2.networksecurity.model.DiscordUserIp;
import io.mamish.serverbot2.networksecurity.model.ManagedSecurityGroup;
import io.mamish.serverbot2.networksecurity.model.PortPermission;
import io.mamish.serverbot2.networksecurity.model.PortProtocol;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedconfig.NetSecConfig;
import io.mamish.serverbot2.sharedutil.IDUtils;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.IpPermission;
import software.amazon.awssdk.services.ec2.model.IpRange;
import software.amazon.awssdk.services.ec2.model.SecurityGroup;

import java.security.Key;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Ec2GroupManager implements IGroupManager {

    private final Ec2Client ec2Client = Ec2Client.create();
    private final String VPCID = CommonConfig.APPLICATION_VPC_ID.getValue();
    private final Crypto crypto;

    public Ec2GroupManager(Crypto crypto) {
        this.crypto = crypto;
    }

    @Override
    public void createGroup(String name) {
        String dataKeyCiphertext = crypto.generateDataKey().snd();
        ec2Client.createSecurityGroup(r ->
                r.vpcId(VPCID)
                .groupName(prependSgPrefix(NetSecConfig.REFERENCE_SG_NAME))
                .description(dataKeyCiphertext));
    }

    @Override
    public void initialiseBaseGroup() {
        ManagedSecurityGroup refGroup = describeGroup(NetSecConfig.REFERENCE_SG_NAME);

        Key dataKey = crypto.decryptDataKey(refGroup.getEncryptedDataKey());
        String markerPlaintext = "MARKER_DO_NOT_REMOVE";
        String markerCiphertext = crypto.encryptLocal(SdkBytes.fromUtf8String(markerPlaintext), dataKey);

        ec2Client.authorizeSecurityGroupIngress(r -> r.groupId(refGroup.getGroupId())
                .ipPermissions(IpPermission.builder()
                        .ipProtocol("icmp")
                        .fromPort(-1)
                        .toPort(-1)
                        .ipRanges(IpRange.builder()
                                .cidrIp("0.0.0.0/32")
                                .description(markerCiphertext)
                                .build())
                        .build()));
    }

    @Override
    public void copyBaseRuleIntoGroup(String name) {
        ManagedSecurityGroup group = describeGroup(name);
        // Copy placeholder rule from reference group
        ManagedSecurityGroup refGroup = describeGroup(NetSecConfig.REFERENCE_SG_NAME);
        // Will always be one and only one rule (barring manual tampering)
        PortPermission referencePort = refGroup.getAllowedPorts().get(0);
        Key dataKey = crypto.decryptDataKey(group.getEncryptedDataKey());

        List<IpRange> newIpRanges = refGroup.getAllowedUsers().stream().map(user -> IpRange.builder()
                .cidrIp(user.getIpAddress()+"/32")
                .description(crypto.encryptLocal(SdkBytes.fromUtf8String(user.getDiscordId()), dataKey))
                .build()
        ).collect(Collectors.toList());

        ec2Client.authorizeSecurityGroupIngress(r -> r.groupId(group.getGroupId()).ipPermissions(IpPermission.builder()
                .ipProtocol(PortProtocol.toLowerCaseName(referencePort.getProtocol()))
                .fromPort(referencePort.getPortRangeFrom())
                .toPort(referencePort.getPortRangeTo())
                .ipRanges(newIpRanges)
                .build()));
    }

    @Override
    public ManagedSecurityGroup describeGroup(String name) throws NoSuchResourceException {
        return makeSimplifiedGroup(findGroup(name));
    }

    @Override
    public List<ManagedSecurityGroup> listGroups() {
        return ec2Client.describeSecurityGroups().securityGroups().stream()
                .filter(sg -> sg.groupName().startsWith(NetSecConfig.SG_NAME_PREFIX))
                .map(this::makeSimplifiedGroup)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteGroup(String name) {
        SecurityGroup group = findGroup(name);
        ec2Client.deleteSecurityGroup(r -> r.groupId(group.groupId()));
    }

    @Override
    public void modifyUserInGroup(ManagedSecurityGroup group, String userAddress, String userId, boolean addNotRemove) {
        if (addNotRemove) {
            Key dataKey = crypto.decryptDataKey(group.getEncryptedDataKey());
            String encryptedUserId = crypto.encryptLocal(SdkBytes.fromUtf8String(userId), dataKey);

            List<IpPermission> newPermissions = group.getAllowedPorts().stream().map(port -> IpPermission.builder()
                    .ipProtocol(PortProtocol.toLowerCaseName(port.getProtocol()))
                    .fromPort(port.getPortRangeFrom())
                    .toPort(port.getPortRangeTo())
                    .ipRanges(IpRange.builder().cidrIp(userAddress+"/32").description(encryptedUserId).build())
                    .build()
            ).collect(Collectors.toList());

            ec2Client.authorizeSecurityGroupIngress(r -> r.groupId(group.getGroupId()).ipPermissions(newPermissions));
        } else {
            group.getAllowedUsers().stream().filter(user -> user.getDiscordId().equals(userId)).findFirst().ifPresent(user ->
                    group.getAllowedPorts().forEach(port ->
                            ec2Client.revokeSecurityGroupIngress(r -> r.groupId(group.getGroupId())
                                    .cidrIp(user.getIpAddress())
                                    .ipProtocol(PortProtocol.toLowerCaseName(port.getProtocol()))
                                    .fromPort(port.getPortRangeFrom())
                                    .toPort(port.getPortRangeTo()))));
        }
    }

    @Override
    public void modifyPortsInGroup(ManagedSecurityGroup group, List<PortPermission> ports, boolean addNotRemove) {
        String gid = group.getGroupId();

        Key dataKey = crypto.decryptDataKey(group.getEncryptedDataKey());

        List<IpRange> allRanges = group.getAllowedUsers().stream().map(u -> IpRange.builder()
                .cidrIp(u.getIpAddress() + "/32")
                .description(crypto.encryptLocal(SdkBytes.fromUtf8String(u.getIpAddress()), dataKey))
                .build()
        ).collect(Collectors.toList());

        List<IpPermission> allPermissions = ports.stream().map(p -> IpPermission.builder()
                .ipProtocol(PortProtocol.toLowerCaseName(p.getProtocol()))
                .fromPort(p.getPortRangeFrom())
                .toPort(p.getPortRangeTo())
                .ipRanges(allRanges)
                .build()
        ).collect(Collectors.toList());

        if (addNotRemove) {
            ec2Client.authorizeSecurityGroupIngress(r -> r.groupId(gid).ipPermissions(allPermissions));
        } else {
            ec2Client.revokeSecurityGroupIngress(r -> r.groupId(gid).ipPermissions(allPermissions));
        }
    }

    private ManagedSecurityGroup makeSimplifiedGroup(SecurityGroup realGroup) {
        String name = realGroup.groupName().substring(NetSecConfig.SG_NAME_PREFIX.length());
        String groupId = realGroup.groupId();
        String encryptedDataKey = realGroup.description();
        List<IpPermission> permissions = realGroup.ipPermissions();

        List<PortPermission> ports;
        List<DiscordUserIp> users;
        if (permissions.isEmpty()) {
            ports = List.of();
            users = List.of();
        } else {
            Stream<PortPermission> tcpPorts = permissions.stream()
                    .filter(p -> p.ipProtocol().equals("tcp"))
                    .map(p -> new PortPermission(PortProtocol.TCP, p.fromPort(), p.toPort()));
            Stream<PortPermission> udpPorts = permissions.stream()
                    .filter(p -> p.ipProtocol().equals("udp"))
                    .map(p -> new PortPermission(PortProtocol.UDP, p.fromPort(), p.toPort()));
            ports = Stream.concat(tcpPorts, udpPorts).collect(Collectors.toList());

            String dataKeyInDescription = realGroup.description();
            Key dataKey = crypto.decryptDataKey(dataKeyInDescription);
            users = permissions.get(0).ipRanges().stream().map(r -> new DiscordUserIp(
                    crypto.decryptLocal(r.description(),dataKey).asUtf8String(),
                    stripCidrMask(r.cidrIp()))
            ).collect(Collectors.toList());
        }

        return new ManagedSecurityGroup(name, groupId, encryptedDataKey, ports, users);
    }

    private SecurityGroup findGroup(String name) throws NoSuchResourceException {
        String exactName = prependSgPrefix(name);
        Filter nameFilter = Filter.builder()
                .name("group-name")
                .values(exactName)
                .build();

        List<SecurityGroup> groups = ec2Client.describeSecurityGroups(r ->
                r.filters(List.of(nameFilter))
        ).securityGroups();

        if (groups.isEmpty()) {
            throw new NoSuchResourceException("No such EC2 security group " + exactName);
        } else {
            return groups.get(0);
        }
    }

    private String prependSgPrefix(String name) {
        return IDUtils.kebab(NetSecConfig.SG_NAME_PREFIX, name);
    }

    private String stripCidrMask(String cidr) {
        return cidr.substring(0, cidr.length() - "/32".length());
    }

}
