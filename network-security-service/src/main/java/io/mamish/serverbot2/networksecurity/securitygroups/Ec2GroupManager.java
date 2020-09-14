package io.mamish.serverbot2.networksecurity.securitygroups;

import io.mamish.serverbot2.framework.exception.server.NoSuchResourceException;
import io.mamish.serverbot2.framework.exception.server.RequestHandlingException;
import io.mamish.serverbot2.framework.exception.server.ResourceAlreadyExistsException;
import io.mamish.serverbot2.networksecurity.crypto.Crypto;
import io.mamish.serverbot2.networksecurity.model.ManagedSecurityGroup;
import io.mamish.serverbot2.networksecurity.model.PortPermission;
import io.mamish.serverbot2.networksecurity.model.PortProtocol;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedconfig.NetSecConfig;
import io.mamish.serverbot2.sharedutil.IDUtils;
import io.mamish.serverbot2.sharedutil.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.security.Key;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Ec2GroupManager implements IGroupManager {

    private final Ec2Client ec2Client = Ec2Client.builder()
            .httpClient(UrlConnectionHttpClient.create())
            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
            .build();
    private final String VPCID = CommonConfig.APPLICATION_VPC_ID.getValue();
    private final String PREFIX_LIST_DATA_KEY_TAG_KEY = "EncryptedDataKey";
    private final Crypto crypto;

    private final Logger logger = LogManager.getLogger(Ec2GroupManager.class);

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
    public void setUserIp(String newUserIpAddress, String userDiscordId) {
        ManagedPrefixList userIpList = getUserIpList();
        Key dataKey = getDataKeyFromPrefixListTags(userIpList);
        String encryptedUserId = crypto.encryptLocal(SdkBytes.fromUtf8String(userDiscordId), dataKey);
        String newUserCidr = newUserIpAddress + "/32";

        // Find the existing user CIDR in the prefix list if it exists
        Optional<String> oExistingUserCidr = ec2Client
                .getManagedPrefixListEntriesPaginator(r -> r.prefixListId(userIpList.prefixListId())).entries().stream()
                .filter(e -> {
                    SdkBytes decryptedUserIdBytes = crypto.decryptLocal(e.description(), dataKey);
                    return decryptedUserIdBytes.asUtf8String().equals(userDiscordId);
                }).findFirst()
                .map(PrefixListEntry::cidr);

        // Prefix list modify call fails if same CIDR is in Add and Remove entries simultaneously,
        // so do nothing if requested CIDR already exists in prefix list.
        // Note: this means we can't easily update descriptions on existing entries, unfortunately.
        if (oExistingUserCidr.isPresent() && oExistingUserCidr.get().equals(newUserCidr)) {

            logger.info("User {} already has CIDR {} whitelisted, no changes required",
                    userDiscordId, newUserCidr);

        } else {

            // Create the basic request to add the user's new CIDR
            ModifyManagedPrefixListRequest baseAddCidrRequest = ModifyManagedPrefixListRequest.builder()
                    .prefixListId(userIpList.prefixListId())
                    .currentVersion(userIpList.version())
                    .addEntries(AddPrefixListEntry.builder()
                            .cidr(newUserCidr)
                            .description(encryptedUserId)
                            .build())
                    .build();

            // If there is an existing CIDR now, it must be different to the new one
            if (oExistingUserCidr.isEmpty()) {

                logger.info("User {} has no existing CIDR - adding {}",
                        userDiscordId, newUserCidr);
                ec2Client.modifyManagedPrefixList(baseAddCidrRequest);

            } else {

                logger.info("User {} has existing CIDR {} - removing it and adding {}",
                        userDiscordId, oExistingUserCidr.get(), newUserCidr);
                ec2Client.modifyManagedPrefixList(baseAddCidrRequest.toBuilder()
                        .removeEntries(RemovePrefixListEntry.builder()
                                .cidr(oExistingUserCidr.get())
                                .build())
                        .build());

            }

        }
    }

    @Override
    public void modifyGroupPorts(ManagedSecurityGroup group, List<PortPermission> ports, boolean addNotRemove) {
        ManagedPrefixList userIpList = getUserIpList();

        PrefixListId prefixListAuthorisation = PrefixListId.builder()
                .prefixListId(userIpList.prefixListId())
                .description("Rule added by NetSec service")
                .build();

        List<IpPermission> allPermissions = ports.stream().map(p -> IpPermission.builder()
                .ipProtocol(PortProtocol.toLowerCaseName(p.getProtocol()))
                .fromPort(p.getPortRangeFrom())
                .toPort(p.getPortRangeTo())
                .prefixListIds(prefixListAuthorisation)
                .build()
        ).collect(Collectors.toList());

        if (addNotRemove) {
            ec2Client.authorizeSecurityGroupIngress(r -> r.groupId(group.getGroupId()).ipPermissions(allPermissions));
        } else {
            ec2Client.revokeSecurityGroupIngress(r -> r.groupId(group.getGroupId()).ipPermissions(allPermissions));
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

    private ManagedPrefixList getUserIpList() {
        Filter nameFilter = Filter.builder().name("prefix-list-name").values(NetSecConfig.USER_IP_PREFIX_LIST_NAME).build();
        ManagedPrefixList userIpList =  ec2Client.describeManagedPrefixLists(r -> r.filters(nameFilter)).prefixLists().get(0);
        putIpListDataKeyIfMissing(userIpList);
        return userIpList;
    }

    private Key getDataKeyFromPrefixListTags(ManagedPrefixList userIpList) {
        Optional<Tag> oTag = userIpList.tags().stream().filter(t -> t.key().equals(PREFIX_LIST_DATA_KEY_TAG_KEY)).findFirst();
        // Create the key if it doesn't exist yet
        Key dataKey;
        if (oTag.isEmpty()) {
            Pair<Key,String> keyPlaintextAndCipherText = crypto.generateDataKey();
            dataKey = keyPlaintextAndCipherText.a();
            Tag newTag = Tag.builder().key(PREFIX_LIST_DATA_KEY_TAG_KEY).value(keyPlaintextAndCipherText.b()).build();
            ec2Client.createTags(r -> r.tags(newTag).resources(userIpList.prefixListId()));
        } else {
            dataKey = crypto.decryptDataKey(oTag.get().value());
        }
        return dataKey;
    }

    private void putIpListDataKeyIfMissing(ManagedPrefixList userIpList) {
        if (userIpList.tags().stream().noneMatch(t -> t.key().equals(PREFIX_LIST_DATA_KEY_TAG_KEY))) {
            String keyCiphertext = crypto.generateDataKey().b();
            Tag newTag = Tag.builder().key(PREFIX_LIST_DATA_KEY_TAG_KEY).value(keyCiphertext).build();
            ec2Client.createTags(r -> r.tags(newTag).resources(userIpList.prefixListId()));
        }
    }

    private String prependSgPrefix(String name) {
        return IDUtils.kebab(NetSecConfig.SG_NAME_PREFIX, name);
    }

    private String stripCidrMask(String cidr) {
        return cidr.substring(0, cidr.length() - "/32".length());
    }

}
