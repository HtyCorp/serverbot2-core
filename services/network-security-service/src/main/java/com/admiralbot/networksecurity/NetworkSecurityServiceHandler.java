package com.admiralbot.networksecurity;

import com.admiralbot.framework.exception.server.NoSuchResourceException;
import com.admiralbot.framework.exception.server.RequestValidationException;
import com.admiralbot.framework.exception.server.ResourceAlreadyExistsException;
import com.admiralbot.framework.exception.server.ResourceExpiredException;
import com.admiralbot.networksecurity.crypto.Crypto;
import com.admiralbot.networksecurity.firewall.DiscordUserAuthInfo;
import com.admiralbot.networksecurity.firewall.DiscordUserAuthType;
import com.admiralbot.networksecurity.firewall.Ec2GroupManager;
import com.admiralbot.networksecurity.firewall.IGroupManager;
import com.admiralbot.networksecurity.model.*;
import com.admiralbot.networksecurity.netanalysis.CloudWatchFlowLogsAnalyser;
import com.admiralbot.networksecurity.netanalysis.INetworkAnalyser;
import com.admiralbot.sharedconfig.CommonConfig;
import com.admiralbot.sharedconfig.NetSecConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.model.KmsException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class NetworkSecurityServiceHandler implements INetworkSecurity {

    private static final Pattern BASIC_IP_REGEX = Pattern.compile("(\\d{1,3}\\.){3}(\\d{1,3})");
    private static final int PLACEHOLDER_AGE_FOR_NO_ACTIVITY = Integer.MAX_VALUE / 2;

    private final Gson gson = new GsonBuilder().serializeNulls().create();
    private final Crypto crypto = new Crypto();
    private final IGroupManager groupManager = chooseGroupManager();
    private final INetworkAnalyser networkAnalyser = chooseNetworkAnalyser();

    private final Logger logger = LogManager.getLogger(NetworkSecurityServiceHandler.class);

    @Override
    public CreateSecurityGroupResponse createSecurityGroup(CreateSecurityGroupRequest request) {

        String name = request.getGameName();
        validateRequestedGameName(name, false);

        try {
            groupManager.createGroup(name);
        } catch (ResourceAlreadyExistsException e) {
            throw new RequestValidationException("A group with this name already exists");
        }
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
            groupManager.modifyGroupPorts(group, addPorts, true);
        }
        if (removePorts != null) {
            groupManager.modifyGroupPorts(group, removePorts, false);
        }

        ManagedSecurityGroup modifiedGroup = groupManager.describeGroup(name);
        return new ModifyPortsResponse(modifiedGroup);
    }

    @Override
    public GenerateIpAuthUrlResponse generateIpAuthUrl(GenerateIpAuthUrlRequest request) {

        DiscordUserAuthInfo newAuthInfo = new DiscordUserAuthInfo(
                2,
                request.getReservationId(),
                Instant.now().getEpochSecond(),
                request.getUserId() == null ? DiscordUserAuthType.GUEST : DiscordUserAuthType.MEMBER,
                request.getUserId()
        );

        String authInfoJson = gson.toJson(newAuthInfo);
        String token = crypto.encrypt(SdkBytes.fromUtf8String(authInfoJson));

        String authUrl = "https://"
                + NetSecConfig.AUTHORIZER_SUBDOMAIN
                + "."
                + CommonConfig.SYSTEM_ROOT_DOMAIN_NAME.getValue()
                + NetSecConfig.AUTHORIZER_PATH_AUTHORIZE
                + "?"+NetSecConfig.AUTHORIZER_PATH_PARAM_TOKEN +"="
                + token; // crypto.encrypt() is already URL-safe
        return new GenerateIpAuthUrlResponse(authUrl);
    }



    @Override
    public AuthorizeIpResponse authorizeIp(AuthorizeIpRequest request) {

        String userAddress = request.getUserIpAddress();

        String authInfoJson;
        try {
            authInfoJson = crypto.decrypt(request.getUserAuthToken()).asUtf8String();
        } catch (KmsException e) {
            throw new RequestValidationException("Provided token is invalid", e);
        }
        DiscordUserAuthInfo authInfo = gson.fromJson(authInfoJson, DiscordUserAuthInfo.class);

        Instant tokenAuthExpireTime = calculateIpAuthExpiryTime(authInfo);
        if (Instant.now().isAfter(tokenAuthExpireTime)) {
            throw new ResourceExpiredException("Token has expired");
        }

        groupManager.setUserIp(userAddress, authInfo);
        return new AuthorizeIpResponse();

    }

    @Override
    public GetAuthorizationByIpResponse getAuthorizationByIp(GetAuthorizationByIpRequest getAuthorizationByIpRequest) {
        return groupManager.getUserInfoByIp(getAuthorizationByIpRequest.getIpAddress())
                .map(info -> new GetAuthorizationByIpResponse(true, calculateIpAuthExpiryTime(info).getEpochSecond())
                ).orElse( new GetAuthorizationByIpResponse(false, 0L));
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

    @Override
    public GetNetworkUsageResponse getNetworkUsage(GetNetworkUsageRequest getNetworkUsageRequest) {
        String requestedEndpointIp = getNetworkUsageRequest.getEndpointVpcIp();
        int requestedWindowSeconds = getNetworkUsageRequest.getWindowSeconds();

        if (!(BASIC_IP_REGEX.matcher(requestedEndpointIp).matches())) {
            throw new RequestValidationException("Requested endpoint IP isn't a valid IPv4 address");
        }
        if (requestedWindowSeconds < 0) {
            throw new RequestValidationException("Analysis window time cannot be negative");
        }

        ManagedSecurityGroup group = groupManager.describeGroup(getNetworkUsageRequest.getTargetSecurityGroupName());

        Optional<Integer> maybeLatestAgeSeconds;
        if (group.getAllowedPorts().isEmpty()) {
            logger.info("Security group has no allowed ports - defaulting to 'no activity' response");
            maybeLatestAgeSeconds = Optional.empty();
        } else if (groupManager.listUserIps().isEmpty()) {
            logger.info("Prefix list has no allowed IPs - defaulting to 'no activity' response");
            maybeLatestAgeSeconds = Optional.empty();
        } else {
            maybeLatestAgeSeconds = networkAnalyser.getLatestActivityAgeSeconds(groupManager.listUserIps(),
                    group.getAllowedPorts(), requestedEndpointIp, requestedWindowSeconds);
            logger.info("Network analyzer reported latest activity age as: {}", maybeLatestAgeSeconds);
        }

        return new GetNetworkUsageResponse(
                maybeLatestAgeSeconds.isPresent(),
                maybeLatestAgeSeconds.orElse(PLACEHOLDER_AGE_FOR_NO_ACTIVITY)
        );
    }

    @Override
    public RevokeExpiredIpsResponse revokeExpiredIps(RevokeExpiredIpsRequest revokeExpiredIpsRequest) {
        groupManager.revokeExpiredIps();
        return new RevokeExpiredIpsResponse();
    }

    private Instant calculateIpAuthExpiryTime(DiscordUserAuthInfo userInfo) {
        Instant tokenAuthIssueInstant = Instant.ofEpochSecond(userInfo.getAuthTimeEpochSeconds());
        switch(userInfo.getAuthType()) {
            case MEMBER:
                return tokenAuthIssueInstant.plus(NetSecConfig.AUTH_URL_MEMBER_TTL);
            case GUEST:
                return tokenAuthIssueInstant.plus(NetSecConfig.AUTH_URL_GUEST_TTL);
            default:
                logger.error("Unexpected IP auth type {}", userInfo.getAuthType());
                throw new IllegalArgumentException("Unexpected data in auth token");
        }
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
        return new Ec2GroupManager(crypto);
    }

    private INetworkAnalyser chooseNetworkAnalyser() {
        return new CloudWatchFlowLogsAnalyser();
    }

}
