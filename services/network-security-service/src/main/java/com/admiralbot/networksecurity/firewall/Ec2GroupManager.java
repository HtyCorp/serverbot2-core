package com.admiralbot.networksecurity.firewall;

import com.admiralbot.framework.exception.server.NoSuchResourceException;
import com.admiralbot.framework.exception.server.RequestHandlingException;
import com.admiralbot.framework.exception.server.ResourceAlreadyExistsException;
import com.admiralbot.framework.exception.server.ServiceLimitException;
import com.admiralbot.nativeimagesupport.cache.ImageCache;
import com.admiralbot.networksecurity.crypto.Crypto;
import com.admiralbot.networksecurity.model.ManagedSecurityGroup;
import com.admiralbot.networksecurity.model.PortPermission;
import com.admiralbot.networksecurity.model.PortProtocol;
import com.admiralbot.sharedconfig.CommonConfig;
import com.admiralbot.sharedconfig.NetSecConfig;
import com.admiralbot.sharedutil.Joiner;
import com.admiralbot.sharedutil.Pair;
import com.admiralbot.sharedutil.SdkUtils;
import com.admiralbot.sharedutil.Utils;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.security.Key;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Ec2GroupManager implements IGroupManager {

    private final Ec2Client ec2Client = SdkUtils.client(Ec2Client.builder());
    private final String VPCID = CommonConfig.APPLICATION_VPC_ID.getValue();
    private final String PREFIX_LIST_DATA_KEY_TAG_KEY = "EncryptedDataKey";

    private final Crypto crypto;
    private final Gson gson = ImageCache.getGson();

    private final Pattern NUMERIC_ID_PATTERN = Pattern.compile("\\d+");

    private final Logger logger = LoggerFactory.getLogger(Ec2GroupManager.class);

    public Ec2GroupManager(Crypto crypto) {
        this.crypto = crypto;
    }

    @Override
    public void createGroup(String name) {
        try {
            ec2Client.createSecurityGroup(r -> r.vpcId(VPCID)
                    .groupName(prependSgPrefix(name))
                    .description("Automatically created security group for application " + name)
            );
        } catch (Ec2Exception e) {
            if (e.awsErrorDetails().errorCode().equals("InvalidGroup.Duplicate")) {
                throw new ResourceAlreadyExistsException("Security group with this name already exists");
            } else {
                throw new RequestHandlingException("Unknown EC2 API exception while creating security group: " + e.getMessage(), e);
            }
        }
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
        try {
            ec2Client.deleteSecurityGroup(r -> r.groupId(group.groupId()));
        } catch (Ec2Exception e) {
            if (e.awsErrorDetails().errorCode().equals("InvalidGroup.NotFound")) {
                throw new NoSuchResourceException("No security group exists with this name");
            } else {
                throw new RequestHandlingException("Unknown EC2 API exception while deleting security group: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public List<String> listUserIps() {
        // EC2 API CIDRs all end in "/32" but we need a plain IP, so just strip the last 3 chars from each.
        return Utils.map(getDecryptedUserList().getEntries(), e -> e.getCidr().substring(0, e.getCidr().length() - 3));
    }

    @Override
    public void setUserIp(String userIpAddress, DiscordUserAuthInfo userInfo) {
        ModifyManagedPrefixListRequest modifyRequest = buildRequestForSetUserIp(userIpAddress, userInfo);
        try {
            ec2Client.modifyManagedPrefixList(modifyRequest);
        } catch (Ec2Exception e) {
            AwsErrorDetails details = e.awsErrorDetails();
            logger.error("Error making PL modify list call, code='{}', message='{}'", details.errorCode(), details.errorMessage(), e);
            throw new RequestHandlingException("Unexpected error when modifying prefix list", e);
        }
    }

    @Override
    public Optional<DiscordUserAuthInfo> getUserInfoByIp(String userIpAddress) {
        DecryptedPrefixList userList = getDecryptedUserList();
        String userCidr = userIpAddress + "/32";
        return userList.getEntries().stream()
                .filter(e -> e.getCidr().equals(userCidr))
                .map(DecryptedPrefixListEntry::getUserInfo)
                .findFirst();
    }

    private ModifyManagedPrefixListRequest buildRequestForSetUserIp(String newUserIpAddress, DiscordUserAuthInfo userInfo) {
        DecryptedPrefixList userList = getDecryptedUserList();
        String encryptedUserInfo = encryptUserInfo(userInfo, userList.getDataKey());
        String newUserCidr = newUserIpAddress + "/32";

        if (cidrExistsInList(userList, newUserCidr)) {
            logger.info("CIDR already exists in list - updating existing entry");
            return buildListModifyRequest(userList, newUserCidr, encryptedUserInfo, null);
        }

        if (userInfo.getAuthType().equals(DiscordUserAuthType.MEMBER)) {
            Optional<String> existingUserCidr = findExistingUserCidr(userList, userInfo.getUserId());
            if (existingUserCidr.isPresent()) {
                logger.info("CIDR {} already exists in list for this user - removing old CIDR and adding new", existingUserCidr.get());
                return buildListModifyRequest(userList, newUserCidr, encryptedUserInfo, existingUserCidr.get());
            }
        }

        if (listIsFull(userList)) {
            String cidrToRemove = getRemovalCandidateCidr(userList);
            logger.info("List is full - removing LRU CIDR {} and adding new", cidrToRemove);
            return buildListModifyRequest(userList, newUserCidr, encryptedUserInfo, cidrToRemove);
        }

        logger.info("Adding new entry without removing any existing ones");
        return buildListModifyRequest(userList, newUserCidr, encryptedUserInfo, null);

    }

    private boolean cidrExistsInList(DecryptedPrefixList userList, String userCidr) {
        return userList.getEntries().stream().anyMatch(e -> e.getCidr().equals(userCidr));
    }

    private Optional<String> findExistingUserCidr(DecryptedPrefixList userList, String userId) {
        return userList.getEntries().stream()
                .filter(e -> Objects.equals(e.getUserInfo().getUserId(), userId))
                .findFirst()
                .map(DecryptedPrefixListEntry::getCidr);
    }

    private boolean listIsFull(DecryptedPrefixList userList) {
        int prefixListCapacity = Integer.parseInt(NetSecConfig.USER_IP_PREFIX_LIST_SIZE.getValue());
        return userList.getEntries().size() >= prefixListCapacity;
    }

    private String getRemovalCandidateCidr(DecryptedPrefixList userList) {

        Optional<String> oldestCidr = userList.getEntries().stream()
                .min(Comparator.comparing(e -> e.getUserInfo().getAuthTimeEpochSeconds()))
                .map(DecryptedPrefixListEntry::getCidr);

        if (oldestCidr.isPresent()) {
            return oldestCidr.get();
        } else {
            throw new IllegalStateException("Can't get removal candidate from an empty prefix list");
        }

    }

    private ModifyManagedPrefixListRequest buildListModifyRequest(DecryptedPrefixList userList, String cidrToAdd,
                                                                  String descriptionToAdd, String cidrToRemove) {
        List<AddPrefixListEntry> addEntries = List.of(AddPrefixListEntry.builder()
                .cidr(cidrToAdd)
                .description(descriptionToAdd)
                .build());
        List<RemovePrefixListEntry> removeEntries = Optional.ofNullable(cidrToRemove)
                .map(cidr -> List.of(RemovePrefixListEntry.builder().cidr(cidr).build()))
                .orElse(null);
        return ModifyManagedPrefixListRequest.builder()
                .prefixListId(userList.getId())
                .currentVersion(userList.getVersion())
                .addEntries(addEntries)
                .removeEntries(removeEntries)
                .build();
    }

    private String encryptUserInfo(DiscordUserAuthInfo userInfo, Key dataKey) {
        String userInfoJson = gson.toJson(userInfo);
        SdkBytes userInfoBytes = SdkBytes.fromUtf8String(userInfoJson);
        return crypto.encryptLocal(userInfoBytes, dataKey);
    }

    private DiscordUserAuthInfo decryptUserInfo(String userInfoCiphertext, Key dataKey) {
        SdkBytes plaintextInfoBytes = crypto.decryptLocal(userInfoCiphertext, dataKey);
        String plaintextInfo = plaintextInfoBytes.asUtf8String();

        // For legacy entries with Discord user IDs only, generate a placeholder entry
        if (NUMERIC_ID_PATTERN.matcher(plaintextInfo).matches()) {
            return new DiscordUserAuthInfo(
                    2,
                    "PLACEHOLDER_RESERVATION_ID",
                    Instant.parse("2020-11-01T00:00:00Z").getEpochSecond(),
                    DiscordUserAuthType.MEMBER,
                    plaintextInfo // In legacy entries, the entry text is just the ID
            );
        }

        // Otherwise this is JSON and can be parsed directly
        return gson.fromJson(plaintextInfo, DiscordUserAuthInfo.class);
    }

    @Override
    public void modifyGroupPorts(ManagedSecurityGroup group, List<PortPermission> ports, boolean addNotRemove) {
        DecryptedPrefixList userIpList = getDecryptedUserList();

        PrefixListId prefixListAuthorisation = PrefixListId.builder()
                .prefixListId(userIpList.getId())
                .description("Rule added by NetSec service")
                .build();

        List<IpPermission> allPermissions = ports.stream().map(p -> IpPermission.builder()
                .ipProtocol(PortProtocol.toLowerCaseName(p.getProtocol()))
                .fromPort(p.getPortRangeFrom())
                .toPort(p.getPortRangeTo())
                .prefixListIds(prefixListAuthorisation)
                .build()
        ).collect(Collectors.toList());

        // Adding ingress rules can cause limit errors, removing rules cannot.
        if (addNotRemove) {
            try {
                ec2Client.authorizeSecurityGroupIngress(r -> r.groupId(group.getGroupId()).ipPermissions(allPermissions));
            } catch (Ec2Exception e) {
                if (e.awsErrorDetails().errorCode().equals("RulesPerSecurityGroupLimitExceeded")) {
                    throw new ServiceLimitException("Cannot add any more ingress rules to security group", e);
                } else {
                    throw new RequestHandlingException("Unknown EC2 API error while adding SG ingress rules: " + e.getMessage(), e);
                }
            }
        } else {
            ec2Client.revokeSecurityGroupIngress(r -> r.groupId(group.getGroupId()).ipPermissions(allPermissions));
        }
    }

    @Override
    public void revokeExpiredIps() {
        DecryptedPrefixList userList = getDecryptedUserList();
        Instant now = Instant.now();

        List<RemovePrefixListEntry> cidrsToRemove = userList.getEntries().stream()
                .filter(e -> {
                    Instant authTime = Instant.ofEpochSecond(e.getUserInfo().getAuthTimeEpochSeconds());
                    Instant expireTime;
                    switch(e.getUserInfo().getAuthType()) {
                        case MEMBER:
                            expireTime = authTime.plus(NetSecConfig.AUTH_URL_MEMBER_TTL); break;
                        case GUEST:
                            expireTime = authTime.plus(NetSecConfig.AUTH_URL_GUEST_TTL); break;
                        default:
                            logger.error("Unexpected IP auth type {}", e.getUserInfo().getAuthType());
                            throw new RequestHandlingException("Unexpected data in auth token");
                    }
                    return now.isAfter(expireTime);
                }).map(e -> RemovePrefixListEntry.builder().cidr(e.getCidr()).build())
                .collect(Collectors.toList());

        logger.info("Got {} expired auth entries to remove", cidrsToRemove.size());

        if (cidrsToRemove.isEmpty()) {
            logger.info("Nothing to do");
        } else {
            ManagedPrefixList newListVersion = ec2Client.modifyManagedPrefixList(r -> r.prefixListId(userList.getId())
                    .currentVersion(userList.getVersion())
                    .removeEntries(cidrsToRemove))
                    .prefixList();
            logger.info("Successfully removed entries to create new list version {}", newListVersion.version());
        }
    }

    private ManagedSecurityGroup makeSimplifiedGroup(SecurityGroup realGroup) {
        // Group name is prefixed with this standard prefix and a joiner, so +1 for length
        String name = realGroup.groupName().substring(NetSecConfig.SG_NAME_PREFIX.length()+1);
        String groupId = realGroup.groupId();
        List<IpPermission> permissions = realGroup.ipPermissions();

        List<PortPermission> ports;
        if (permissions.isEmpty()) {
            ports = List.of();
        } else {
            ports = permissions.stream().map(p -> {
                PortProtocol protocol;
                try {
                    protocol = PortProtocol.fromLowerCaseName(p.ipProtocol());
                    return new PortPermission(protocol, p.fromPort(), p.toPort());
                } catch (IllegalArgumentException e) {
                    throw new RequestHandlingException("Unexpected protocol name '" + p.ipProtocol() + "' in EC2 permission", e);
                }
            }).collect(Collectors.toList());
        }

        return new ManagedSecurityGroup(name, groupId, ports);
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

    private DecryptedPrefixList getDecryptedUserList() {
        Filter nameFilter = Filter.builder().name("prefix-list-name").values(NetSecConfig.USER_IP_PREFIX_LIST_NAME).build();
        ManagedPrefixList ec2List =  ec2Client.describeManagedPrefixLists(r -> r.filters(nameFilter)).prefixLists().get(0);

        Key dataKey = getDataKeyFromPrefixListTags(ec2List);

        List<DecryptedPrefixListEntry> decryptedEntries = ec2Client.getManagedPrefixListEntriesPaginator(r -> r.prefixListId(ec2List.prefixListId()))
                .entries().stream()
                .map(e -> new DecryptedPrefixListEntry(e.cidr(), decryptUserInfo(e.description(), dataKey)))
                .collect(Collectors.toList());

        return new DecryptedPrefixList(ec2List.prefixListId(), ec2List.version(), dataKey, decryptedEntries);
    }

    private Key getDataKeyFromPrefixListTags(ManagedPrefixList userIpList) {
        Optional<Tag> oTag = userIpList.tags().stream().filter(t -> t.key().equals(PREFIX_LIST_DATA_KEY_TAG_KEY)).findFirst();
        // Create the key if it doesn't exist yet
        Key dataKey;
        if (oTag.isEmpty()) {
            Pair<Key, String> keyPlaintextAndCipherText = crypto.generateDataKey();
            dataKey = keyPlaintextAndCipherText.a();
            Tag newTag = Tag.builder().key(PREFIX_LIST_DATA_KEY_TAG_KEY).value(keyPlaintextAndCipherText.b()).build();
            ec2Client.createTags(r -> r.tags(newTag).resources(userIpList.prefixListId()));
        } else {
            dataKey = crypto.decryptDataKey(oTag.get().value());
        }
        return dataKey;
    }

    private String prependSgPrefix(String name) {
        return Joiner.kebab(NetSecConfig.SG_NAME_PREFIX, name);
    }

}
