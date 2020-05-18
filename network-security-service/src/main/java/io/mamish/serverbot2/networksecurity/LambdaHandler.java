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
import software.amazon.awssdk.services.ec2.model.IpPermission;
import software.amazon.awssdk.services.ec2.model.IpRange;
import software.amazon.awssdk.services.ec2.model.SecurityGroup;
import software.amazon.awssdk.services.kms.model.KmsException;

import java.security.Key;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LambdaHandler extends LambdaApiServer<INetworkSecurity> implements INetworkSecurity {

    private final Ec2Client ec2Client = Ec2Client.create();
    private final CryptoHelper crypto = new CryptoHelper();
    private final String VPCID = CommonConfig.APPLICATION_VPC_ID.getValue();

    public LambdaHandler() {
        // Create reference group if missing. Should optimise this later to make it once-only.
        try {
            getManagedGroup(NetSecConfig.REFERENCE_SG_SUFFIX);
        } catch (RequestHandlingException e) {
            String dataKeyString = crypto.generateDataKey();
            Key dataKey = crypto.decryptDataKey(dataKeyString);

            String groupId = ec2Client.createSecurityGroup(r -> r.vpcId(VPCID)
                    .groupName(makeSgName(NetSecConfig.REFERENCE_SG_SUFFIX))
                    .description(dataKeyString)
            ).groupId();

            // Create a common ICMP rule so there's at least one IP and rule in the system to copy
            ec2Client.authorizeSecurityGroupIngress(r -> r.groupId(groupId)
                    .ipPermissions(IpPermission.builder()
                            .ipProtocol("icmp")
                            .fromPort(-1)
                            .toPort(-1)
                            .ipRanges(IpRange.builder()
                                    .cidrIp("0.0.0.0/32")
                                    .description(crypto.encryptLocal(SdkBytes.fromUtf8String("REFERENCE"), dataKey))
                                    .build())
                            .build()));
        }
    }

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

        String dataKeyString = crypto.generateDataKey();
        Key dataKey = crypto.decryptDataKey(dataKeyString);
        String newFullName = makeSgName(gameName);
        String newId;
        try {
            newId = ec2Client.createSecurityGroup(r ->
                    r.groupName(newFullName)
                    .description(dataKeyString)
                    .vpcId(VPCID)
            ).groupId();
        } catch (AwsServiceException e) {
            if (e.awsErrorDetails().errorCode().equals("InvalidGroup.Duplicate")) {
                throw new RequestHandlingException("Group for name '" + gameName + "' already exists");
            }
            e.printStackTrace();
            throw new RequestHandlingRuntimeException("Could not create new security group", e);
        }

        // Copy placeholder rule from reference group
        ManagedSecurityGroup referenceGroup = getManagedGroup(NetSecConfig.REFERENCE_SG_SUFFIX);
        // Per the initial definition in constructor, will always be one and only one rule
        PortPermission referencePort = referenceGroup.getAllowedPorts().get(0);

        List<IpRange> newIpRanges = referenceGroup.getAllowedUsers().stream().map(user -> IpRange.builder()
                .cidrIp(user.getIpAddress()+"/32")
                .description(crypto.encryptLocal(SdkBytes.fromUtf8String(user.getDiscordId()), dataKey))
                .build()
        ).collect(Collectors.toList());

        ec2Client.authorizeSecurityGroupIngress(r -> r.groupId(newId).ipPermissions(IpPermission.builder()
                .ipProtocol(PortProtocol.toEc2ApiName(referencePort.getProtocol()))
                .fromPort(referencePort.getPortRangeFrom())
                .toPort(referencePort.getPortRangeTo())
                .ipRanges(newIpRanges)
                .build()));

        ManagedSecurityGroup finalGroup = getManagedGroup(gameName);
        return new CreateSecurityGroupResponse(finalGroup);
    }

    @Override
    public DescribeSecurityGroupResponse describeSecurityGroup(DescribeSecurityGroupRequest request) {
        String gameName = request.getGameName();
        validateRequestedGameName(gameName);

        ManagedSecurityGroup simplifiedGroup = getManagedGroup(gameName);
        return new DescribeSecurityGroupResponse(simplifiedGroup);

    }

    @Override
    public ModifyPortsResponse modifyPorts(ModifyPortsRequest request) {
        String gameName = request.getGameName();
        validateRequestedGameName(gameName);

        ManagedSecurityGroup currentGroup = getManagedGroup(gameName);
        String realGroupId = currentGroup.getGroupId();

        boolean icmpChanges = Stream.concat(request.getAddPorts().stream(), request.getRemovePorts().stream())
                .anyMatch(p -> p.getProtocol().equals(PortProtocol.ICMP));
        if (icmpChanges) {
            throw new RequestValidationException("Cannot modify ICMP rules");
        }

        Key dataKey = crypto.decryptDataKey(currentGroup.getEncryptedDataKey());
        List<IpPermission> permissionsToRemove = buildIpPermissions(currentGroup, request.getRemovePorts(), dataKey);
        List<IpPermission> permissionsToAdd = buildIpPermissions(currentGroup, request.getAddPorts(), dataKey);

        ec2Client.revokeSecurityGroupIngress(r -> r.groupId(realGroupId).ipPermissions(permissionsToRemove));
        ec2Client.authorizeSecurityGroupIngress(r -> r.groupId(realGroupId).ipPermissions(permissionsToAdd));

        ManagedSecurityGroup modifiedGroup = getManagedGroup(gameName);
        return new ModifyPortsResponse(modifiedGroup);

    }

    private List<IpPermission> buildIpPermissions(ManagedSecurityGroup group, List<PortPermission> ports, Key dataKey) {

        List<IpRange> allRanges = group.getAllowedUsers().stream().map(u -> IpRange.builder()
                .cidrIp(u.getIpAddress() + "/32")
                .description(crypto.encryptLocal(SdkBytes.fromUtf8String(u.getIpAddress()), dataKey))
                .build()
        ).collect(Collectors.toList());

        return ports.stream().map(p -> IpPermission.builder()
                .ipProtocol(PortProtocol.toEc2ApiName(p.getProtocol()))
                .fromPort(p.getPortRangeFrom())
                .toPort(p.getPortRangeTo())
                .ipRanges(allRanges)
                .build()
        ).collect(Collectors.toList());
    }

    @Override
    public GenerateIpAuthUrlResponse generateIpAuthUrl(GenerateIpAuthUrlRequest request) {
        String token = crypto.encrypt(SdkBytes.fromUtf8String(request.getUserId()));
        String authUrl = "https://"
                + NetSecConfig.AUTH_SUBDOMAIN
                + CommonConfig.APEX_DOMAIN_NAME
                + NetSecConfig.AUTH_PATH
                + "?token="
                + token;
        return new GenerateIpAuthUrlResponse(authUrl);
    }

    @Override
    public AuthorizeIpResponse authorizeIp(AuthorizeIpRequest request) {
        String ipAddress = request.getUserIpAddress();

        if (request.getEncryptedUserId() == null && request.getUserId() == null) {
            throw new RequestValidationException("Must specify either encryptedUserId or userId");
        }
        if (request.getEncryptedUserId() != null && request.getUserId() != null) {
            throw new RequestValidationException("Must specify only one of encryptedUserId or userId");
        }

        final String userId;
        if (request.getUserId() != null) {
            userId = request.getUserId();
        } else {
            try {
                userId = crypto.decrypt(request.getEncryptedUserId()).asUtf8String();
            } catch (KmsException e) {
                e.printStackTrace();
                throw new RequestValidationException("Provided token is invalid", e);
            }
        }

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
                        .ipRanges(IpRange.builder().cidrIp(ipAddress+"/32").description(encryptedUserId).build())
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
                    stripCidrMask(r.cidrIp()))
            ).collect(Collectors.toList());
        }

        return new ManagedSecurityGroup(gameName, fullName, groupId, encryptedDataKey, ports, users);
    }

    private String stripCidrMask(String cidr) {
        return cidr.substring(0, cidr.length() - "/32".length());
    }

    private List<ManagedSecurityGroup> getAllManagedGroups() {
        return ec2Client.describeSecurityGroups().securityGroups().stream()
                .filter(sg -> sg.groupName().startsWith(NetSecConfig.SG_NAME_PREFIX))
                .map(this::simplifyGroup)
                .collect(Collectors.toList());
    }

    private ManagedSecurityGroup getManagedGroup(String gameName) {
        return simplifyGroup(getRealGroup(gameName));
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
