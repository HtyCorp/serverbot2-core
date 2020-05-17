package io.mamish.serverbot2.networksecurity;

import io.mamish.serverbot2.framework.exception.server.RequestHandlingException;
import io.mamish.serverbot2.framework.exception.server.RequestHandlingRuntimeException;
import io.mamish.serverbot2.framework.server.LambdaApiServer;
import io.mamish.serverbot2.networksecurity.model.*;
import io.mamish.serverbot2.sharedconfig.NetSecConfig;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.IpPermission;
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
        String name = request.getGameName();
        try {
            String dataKeyString = crypto.generateDataKey();
            String id = ec2Client.createSecurityGroup(r -> r.groupName(makeSgName(name)).description(dataKeyString)).groupId();
            ApplicationSecurityGroup blank = new ApplicationSecurityGroup(name, id, List.of(), List.of());
            return new CreateSecurityGroupResponse(blank);
        } catch (AwsServiceException e) {
            if (e.awsErrorDetails().errorCode().equals("InvalidGroup.Duplicate")) {
                throw new RequestHandlingException("Group for name '" + name + "' already exists.");
            }
            e.printStackTrace();
            throw new RequestHandlingRuntimeException("Could not creatte new security group", e);
        }
    }

    // TODO

    @Override
    public DescribeSecurityGroupResponse describeSecurityGroup(DescribeSecurityGroupRequest request) {
        String gameName = request.getGameName();

        SecurityGroup realGroup = getRealGroup(gameName);
        List<IpPermission> permissions = realGroup.ipPermissions();
        String groupId = realGroup.groupId();

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

        return new DescribeSecurityGroupResponse(new ApplicationSecurityGroup(gameName, groupId, ports, users));

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
        return null;
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

    private static String makeSgName(String gameName) {
        return NetSecConfig.SG_NAME_PREFIX + "-" + gameName;
    }

}
