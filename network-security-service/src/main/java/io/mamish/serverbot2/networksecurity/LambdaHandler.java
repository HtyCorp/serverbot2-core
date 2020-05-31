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
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.model.KmsException;

import java.util.List;
import java.util.stream.Stream;

public class LambdaHandler extends LambdaApiServer<INetworkSecurity> implements INetworkSecurity {

    private final String VPCID = CommonConfig.APPLICATION_VPC_ID.getValue();
    private final Crypto crypto = new Crypto();
    private final IGroupManager groupManager = chooseGroupManager();

    public LambdaHandler() {
        // Create reference group if missing. Should optimise this later to make it once-only.
        try {
            groupManager.describeGroup(NetSecConfig.REFERENCE_SG_NAME);
        } catch (NoSuchResourceException e) {
            groupManager.createGroup(NetSecConfig.REFERENCE_SG_NAME);
            groupManager.initialiseBaseGroup();
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

        String name = request.getGameName();
        validateRequestedGameName(name);
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
        validateRequestedGameName(gameName);

        ManagedSecurityGroup simplifiedGroup = getGroupOrThrow(gameName);
        return new DescribeSecurityGroupResponse(simplifiedGroup);

    }

    @Override
    public ModifyPortsResponse modifyPorts(ModifyPortsRequest request) {
        String name = request.getGameName();
        validateRequestedGameName(name);

        ManagedSecurityGroup group = groupManager.describeGroup(name);

        boolean icmpChanges = Stream.concat(request.getAddPorts().stream(), request.getRemovePorts().stream())
                .anyMatch(p -> p.getProtocol().equals(PortProtocol.ICMP));
        if (icmpChanges) {
            throw new RequestValidationException("Cannot modify ICMP rules");
        }

        groupManager.modifyPortsInGroup(group, request.getAddPorts(), true);
        groupManager.modifyPortsInGroup(group, request.getRemovePorts(), false);

        ManagedSecurityGroup modifiedGroup = groupManager.describeGroup(name);
        return new ModifyPortsResponse(modifiedGroup);
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

        groups.forEach(g -> {
            groupManager.modifyUserInGroup(g, userAddress, userId, false);
            groupManager.modifyUserInGroup(g, userAddress, userId, true);
        });

        return new AuthorizeIpResponse();

    }

    @Override
    public DeleteSecurityGroupResponse deleteSecurityGroup(DeleteSecurityGroupRequest request) {
        String name = request.getGameName();
        validateRequestedGameName(name);

        ManagedSecurityGroup group = groupManager.describeGroup(name);
        groupManager.deleteGroup(name);

        return new DeleteSecurityGroupResponse(group.getGroupId());
    }

    private ManagedSecurityGroup getGroupOrThrow(String name) throws NoSuchResourceException {
        return groupManager.describeGroup(name);
    }

    private void validateRequestedGameName(String name) {
        boolean inReservedNames = CommonConfig.RESERVED_APP_NAMES.contains(name);
        if (inReservedNames) {
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
