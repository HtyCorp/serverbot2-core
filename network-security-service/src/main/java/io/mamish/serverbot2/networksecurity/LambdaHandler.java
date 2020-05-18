package io.mamish.serverbot2.networksecurity;

import io.mamish.serverbot2.framework.exception.server.RequestHandlingException;
import io.mamish.serverbot2.framework.exception.server.RequestHandlingRuntimeException;
import io.mamish.serverbot2.framework.exception.server.RequestValidationException;
import io.mamish.serverbot2.framework.server.LambdaApiServer;
import io.mamish.serverbot2.networksecurity.model.*;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedconfig.NetSecConfig;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
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

public class LambdaHandler extends LambdaApiServer<INetworkSecurity> implements INetworkSecurity {

    private final Ec2Client ec2Client = Ec2Client.create();
    private final CryptoHelper crypto = new CryptoHelper();

    @Override
    protected Class<INetworkSecurity> getModelClass() {
        return INetworkSecurity.class;
    }

    @Override
    protected INetworkSecurity getHandlerInstance() {
        return this;
    }

    @Override
    public CreateSecurityGroupResponse createSecurityGroup(CreateSecurityGroupRequest request) {
        String gameName = request.getGameName();
        validateRequestedGameName(gameName);

        ec2Client.describeVpcs(r -> r.filters(Filter.builder().name()))

        try {
            String dataKeyString = crypto.generateDataKey();
            String newFullName = makeSgName(gameName);
            String id = ec2Client.createSecurityGroup(r -> r.groupName(newFullName).description(dataKeyString)).groupId();
            ManagedSecurityGroup blank = new ManagedSecurityGroup(gameName, newFullName, id, dataKeyString, List.of(), List.of());
            return new CreateSecurityGroupResponse(blank);
        } catch (AwsServiceException e) {
            if (e.awsErrorDetails().errorCode().equals("InvalidGroup.Duplicate")) {
                throw new RequestHandlingException("Group for name '" + gameName + "' already exists");
            }
            e.printStackTrace();
            throw new RequestHandlingRuntimeException("Could not create new security group", e);
        }
    }

    // TODO

    @Override
    public DescribeSecurityGroupResponse describeSecurityGroup(DescribeSecurityGroupRequest request) {
        String gameName = request.getGameName();
        validateRequestedGameName(gameName);

        SecurityGroup realGroup = getRealGroup(gameName);
        List<IpPermission> permissions = realGroup.ipPermissions();
        String groupId = realGroup.groupId();

        ManagedSecurityGroup simplifiedGroup = simplifyGroup(realGroup);
        return new DescribeSecurityGroupResponse(simplifiedGroup);

    }

    @Override
    public ModifyPortsResponse modifyPorts(ModifyPortsRequest request) {
        return null;
    }

    @Override
    public GenerateIpAuthUrlResponse generateIpAuthUrl(GenerateIpAuthUrlRequest request) {
        return null;
    }

    @Override
    public AuthorizeIpResponse authorizeIp(AuthorizeIpRequest request) {
        String userId = request.getUserId();
        String ipAddress = request.getUserIpAddress();

        List<ManagedSecurityGroup> managedGroups = getAllManagedGroups();

        // For every group the user is authorized in (which should be all or none), revoke their existing IP address
        managedGroups.forEach(sg ->
                sg.getAllowedUsers().stream().filter(user -> user.getDiscordId().equals(userId)).findFirst().ifPresent(user ->
                        sg.getAllowedPorts().forEach(port ->
                                ec2Client.revokeSecurityGroupIngress(r -> r.groupId(sg.getGroupId())
                                        .cidrIp(user.getIpAddress())
                                        .ipProtocol(PortProtocol.toEc2ApiName(port.getProtocol()))
                                        .fromPort(port.getPortRangeFrom())
                                        .toPort(port.getPortRangeTo())))));

        // For every group, authorize the new IP address
        managedGroups.forEach(sg -> {
                Key dataKey = crypto.decryptDataKey(sg.getEncryptedDataKey());
                String encryptedUserId = crypto.encryptLocal(SdkBytes.fromUtf8String(userId), dataKey);

                List<IpPermission> newPermissions = sg.getAllowedPorts().stream().map(port -> IpPermission.builder()
                        .ipProtocol(PortProtocol.toEc2ApiName(port.getProtocol()))
                        .fromPort(port.getPortRangeFrom())
                        .toPort(port.getPortRangeTo())
                        .ipRanges(IpRange.builder().cidrIp(ipAddress).description(encryptedUserId).build())
                        .build()
                ).collect(Collectors.toList());

                ec2Client.authorizeSecurityGroupIngress(r -> r.groupId(sg.getGroupId()).ipPermissions(newPermissions));
        });

        return new AuthorizeIpResponse();

    }

    private ManagedSecurityGroup simplifyGroup(SecurityGroup realGroup) {

        if (!realGroup.groupName().startsWith(NetSecConfig.SG_NAME_PREFIX)) {
            throw new IllegalArgumentException("Group '" + realGroup.groupName() + "' is not a managed security group.");
        }

        String gameName = realGroup.groupName().substring(NetSecConfig.SG_NAME_PREFIX.length());
        String fullName = realGroup.groupName();
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
                    r.cidrIp())
            ).collect(Collectors.toList());
        }

        return new ManagedSecurityGroup(gameName, fullName, groupId, encryptedDataKey, ports, users);
    }

    private List<ManagedSecurityGroup> getAllManagedGroups() {
        return ec2Client.describeSecurityGroups().securityGroups().stream()
                .filter(sg -> sg.groupName().startsWith(NetSecConfig.SG_NAME_PREFIX))
                .map(this::simplifyGroup)
                .collect(Collectors.toList());
    }

    private SecurityGroup getRealGroup(String gameName) {
        String fullName = makeSgName(gameName);
        List<SecurityGroup> groups = ec2Client.describeSecurityGroups(r -> r.groupNames(fullName)).securityGroups();
        if (groups.isEmpty()) {
            throw new RequestHandlingException("No security group found for game ID '" + gameName
                    + "' (full name '" + fullName + "')");
        }
        return groups.get(0);
    }

    private void validateRequestedGameName(String name) {
        boolean inReservedNames = CommonConfig.RESERVED_APP_NAMES.contains(name);
        if (inReservedNames) {
            throw new RequestValidationException("Requested name '" + name + "' is reserved");
        }
    }

    private static String makeSgName(String gameName) {
        return NetSecConfig.SG_NAME_PREFIX + gameName;
    }

}
