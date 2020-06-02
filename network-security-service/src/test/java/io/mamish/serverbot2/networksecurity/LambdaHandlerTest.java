package io.mamish.serverbot2.networksecurity;

import io.mamish.serverbot2.networksecurity.model.*;
import io.mamish.serverbot2.sharedconfig.NetSecConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LambdaHandlerTest {

    private static final PortPermission SAMPLE_PORT = new PortPermission(PortProtocol.TCP, 7777, 7777);
    private static final DiscordUserIp SAMPLE_USER = new DiscordUserIp("12345", "1.2.3.4");

    private static final PortPermission REF_PORT = new PortPermission(PortProtocol.ICMP, -1, -1);
    private static final DiscordUserIp REF_USER = new DiscordUserIp("REFERENCE_DO_NOT_DELETE", "0.0.0.0");

    private NetworkSecurityServiceHandler handler;

    @BeforeEach
    void setUp() {
        handler = new NetworkSecurityServiceHandler();
    }

    @Test
    void testFirstRunReferenceSetup() {

        // Reference creation should have run during handler instantiation

        ManagedSecurityGroup refGroup = handler.describeSecurityGroup(
                new DescribeSecurityGroupRequest(NetSecConfig.REFERENCE_SG_NAME)
        ).getSecurityGroup();

        Assertions.assertEquals(1, refGroup.getAllowedPorts().size());
        Assertions.assertEquals(1, refGroup.getAllowedUsers().size());

        PortPermission port = refGroup.getAllowedPorts().get(0);
        DiscordUserIp user = refGroup.getAllowedUsers().get(0);

        Assertions.assertEquals(port, REF_PORT);
        Assertions.assertEquals(user, REF_USER);

    }

    @Test
    void testNewGroupRuleCopy() {

        handler.authorizeIp(
                new AuthorizeIpRequest(SAMPLE_USER.getIpAddress(), null, SAMPLE_USER.getDiscordId())
        );

        ManagedSecurityGroup refGroup = handler.describeSecurityGroup(
                new DescribeSecurityGroupRequest(NetSecConfig.REFERENCE_SG_NAME)
        ).getSecurityGroup();

        Assertions.assertTrue(refGroup.getAllowedPorts().contains(REF_PORT));
        Assertions.assertTrue(refGroup.getAllowedUsers().contains(SAMPLE_USER));

        ManagedSecurityGroup newGroup = handler.createSecurityGroup(
                new CreateSecurityGroupRequest("testname")
        ).getCreatedGroup();

        Assertions.assertTrue(refGroup.getAllowedPorts().contains(REF_PORT));
        Assertions.assertTrue(refGroup.getAllowedUsers().contains(SAMPLE_USER));

        Assertions.assertTrue(newGroup.getAllowedPorts().contains(REF_PORT));
        Assertions.assertTrue(newGroup.getAllowedUsers().contains(SAMPLE_USER));

    }

    @Test
    void testAuthUrlFlow() {

        handler.createSecurityGroup(new CreateSecurityGroupRequest("testname"));
        ManagedSecurityGroup group = handler.modifyPorts(new ModifyPortsRequest(
                "testname",
                List.of(SAMPLE_PORT),
                null)
        ).getModifiedGroup();

        Assertions.assertTrue(group.getAllowedPorts().contains(SAMPLE_PORT));

        String authUrl = handler.generateIpAuthUrl(
                new GenerateIpAuthUrlRequest(SAMPLE_USER.getDiscordId())
        ).getIpAuthUrl();

        Pattern tokenPattern = Pattern.compile("\\?token=(.*)$");
        Matcher matcher = tokenPattern.matcher(authUrl);
        Assertions.assertTrue(matcher.find());
        String token = matcher.group(1);

        handler.authorizeIp(new AuthorizeIpRequest(SAMPLE_USER.getIpAddress(), token, null));

        ManagedSecurityGroup updated = handler.describeSecurityGroup(
                new DescribeSecurityGroupRequest("testname")
        ).getSecurityGroup();

        Assertions.assertTrue(updated.getAllowedUsers().contains(SAMPLE_USER));

    }
}
