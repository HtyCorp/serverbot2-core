package io.mamish.serverbot2.networksecurity;

import io.mamish.serverbot2.framework.exception.server.NoSuchResourceException;
import io.mamish.serverbot2.framework.exception.server.RequestValidationException;
import io.mamish.serverbot2.framework.server.LambdaApiServer;
import io.mamish.serverbot2.networksecurity.crypto.Crypto;
import io.mamish.serverbot2.networksecurity.model.*;
import io.mamish.serverbot2.networksecurity.securitygroups.Ec2GroupManager;
import io.mamish.serverbot2.networksecurity.securitygroups.IGroupManager;
import io.mamish.serverbot2.networksecurity.securitygroups.MockGroupManager;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedconfig.NetSecConfig;
import io.mamish.serverbot2.sharedutil.LogUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.model.KmsException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class NetworkSecurityServiceHandler implements INetworkSecurity {

    private final Logger logger = LogManager.getLogger(NetworkSecurityServiceHandler.class);

    private final Crypto crypto = new Crypto();
    private final IGroupManager groupManager = chooseGroupManager();

    public NetworkSecurityServiceHandler() {
        // Create reference group if missing. Should optimise this later to make it once-only.
        try {
            groupManager.describeGroup(NetSecConfig.REFERENCE_SG_NAME);
        } catch (NoSuchResourceException e) {
            groupManager.createGroup(NetSecConfig.REFERENCE_SG_NAME);
            groupManager.initialiseBaseGroup();
        }
    }

    @Override
    public CreateSecurityGroupResponse createSecurityGroup(CreateSecurityGroupRequest request) {

        String name = request.getGameName();
        validateRequestedGameName(name, false);
        try {
            groupManager.describeGroup(name);
            throw new RequestValidationException("A group with this name already exists");
        } catch (NoSuchResourceException e) {
            // Expected, carry on...
        }

        groupManager.createGroup(name);
        groupManager.copyBaseRuleIntoGroup(name);
        ManagedSecurityGroup result = groupManager.describeGroup(name);
        return new CreateSecurityGroupResponse(result);

    }

    @Override
    public DescribeSecurityGroupResponse describeSecurityGroup(DescribeSecurityGroupRequest request) {
        String gameName = request.getGameName();
        validateRequestedGameName(gameName, true);

        ManagedSecurityGroup simplifiedGroup = getGroupOrThrow(gameName);
        return new DescribeSecurityGroupResponse(simplifiedGroup);

    }

    @Override
    public ModifyPortsResponse modifyPorts(ModifyPortsRequest request) {
        String name = request.getGameName();
        validateRequestedGameName(name, false);

        List<PortPermission> addPorts = request.getAddPorts();
        List<PortPermission> removePorts = request.getRemovePorts();

        ManagedSecurityGroup group = groupManager.describeGroup(name);

        boolean icmpChanges = Stream.concat(
                (addPorts == null) ? Stream.empty() : addPorts.stream(),
                (removePorts == null) ? Stream.empty() : removePorts.stream()
        ).anyMatch(p -> p.getProtocol().equals(PortProtocol.ICMP));
        if (icmpChanges) {
            throw new RequestValidationException("Cannot modify ICMP rules");
        }

        if (addPorts != null) {
            groupManager.modifyPortsInGroup(group, addPorts, true);
        }
        if (removePorts != null) {
            groupManager.modifyPortsInGroup(group, removePorts, false);
        }

        ManagedSecurityGroup modifiedGroup = groupManager.describeGroup(name);
        return new ModifyPortsResponse(modifiedGroup);
    }

    @Override
    public GenerateIpAuthUrlResponse generateIpAuthUrl(GenerateIpAuthUrlRequest request) {
        String token = crypto.encrypt(SdkBytes.fromUtf8String(request.getUserId()));
        String authUrl = "https://"
                + NetSecConfig.AUTH_SUBDOMAIN
                + "."
                + CommonConfig.APEX_DOMAIN_NAME
                + NetSecConfig.AUTH_PATH
                + "?token="
                + URLEncoder.encode(token, StandardCharsets.UTF_8);
        return new GenerateIpAuthUrlResponse(authUrl);
    }

    @Override
    public AuthorizeIpResponse authorizeIp(AuthorizeIpRequest request) {
        String userAddress = request.getUserIpAddress();

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

        List<ManagedSecurityGroup> groups = groupManager.listGroups();

        LogUtils.debugDump(logger, "All security groups from manager:", groups);

        groups.forEach(g -> {
            groupManager.removeUserFromGroup(g, userId);
            groupManager.addUserToGroup(g, userAddress, userId);
        });

        return new AuthorizeIpResponse();

    }

    @Override
    public DeleteSecurityGroupResponse deleteSecurityGroup(DeleteSecurityGroupRequest request) {
        String name = request.getGameName();
        validateRequestedGameName(name, false);

        ManagedSecurityGroup group = groupManager.describeGroup(name);
        groupManager.deleteGroup(name);

        return new DeleteSecurityGroupResponse(group.getGroupId());
    }

    private ManagedSecurityGroup getGroupOrThrow(String name) throws NoSuchResourceException {
        return groupManager.describeGroup(name);
    }

    private void validateRequestedGameName(String name, boolean allowReserved) {
        Pattern NAME_REGEX = CommonConfig.APP_NAME_REGEX;
        if (!NAME_REGEX.matcher(name).matches()) {
            throw new RequestValidationException("Name '" + name + "' is not a valid name (allowed regex: " + NAME_REGEX.pattern() + ")");
        }
        boolean inReservedNames = CommonConfig.RESERVED_APP_NAMES.contains(name);
        if (!allowReserved && inReservedNames) {
            throw new RequestValidationException("Requested name '" + name + "' is reserved");
        }
    }

    private IGroupManager chooseGroupManager() {
        if (CommonConfig.ENABLE_MOCK.notNull()) {
            return new MockGroupManager(crypto);
        } else {
            return new Ec2GroupManager(crypto);
        }
    }

}
