package io.mamish.serverbot2.workflow;

import io.mamish.serverbot2.gamemetadata.model.GameMetadata;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedutil.IDUtils;
import io.mamish.serverbot2.sharedutil.LogUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.route53.Route53Client;
import software.amazon.awssdk.services.route53.model.*;

public class AppDnsRecordManager {

    private static final String HOSTED_ZONE_ID = CommonConfig.HOSTED_ZONE_ID.getValue();

    private final Ec2Client ec2Client = Ec2Client.create();
    private final Route53Client route53Client = Route53Client.builder()
            .region(Region.AWS_GLOBAL)
            .build();

    private final Logger logger = LogManager.getLogger(AppDnsRecordManager.class);

    public String updateAppRecordAndGetLocationString(GameMetadata metadata) {
        return changeRecordForApp(metadata, ChangeAction.UPSERT);
    }

    public void deleteAppRecord(GameMetadata metadata) {
        changeRecordForApp(metadata, ChangeAction.DELETE);
    }

    private String changeRecordForApp(GameMetadata metadata, ChangeAction action) {
        ChangeBatch changeBatch = buildChangeBatch(metadata, action);
        LogUtils.debugDump(logger, "Dumping pending resource " + action + " change batch:", changeBatch);
        route53Client.changeResourceRecordSets(r -> r.hostedZoneId(HOSTED_ZONE_ID).changeBatch(changeBatch));
        return makeLocationString(changeBatch);
    }

    private String makeLocationString(ChangeBatch changeBatch) {
        ResourceRecordSet recordSet = changeBatch.changes().get(0).resourceRecordSet();
        String fqdn = recordSet.name();
        String ip = recordSet.resourceRecords().get(0).value();
        return fqdn + " (" + ip + ")";
    }

    private ChangeBatch buildChangeBatch(GameMetadata metadata, ChangeAction action) {
        ResourceRecordSet recordSet = buildRecordSet(metadata);
        Change change = Change.builder()
                .action(action)
                .resourceRecordSet(recordSet)
                .build();
        return ChangeBatch.builder()
                .changes(change)
                .build();
    }

    private ResourceRecordSet buildRecordSet(GameMetadata metadata) {
        String publicIp = describeInstance(metadata).publicIpAddress();
        if (publicIp == null) {
            throw new IllegalStateException("Instance id " + metadata.getInstanceId() + " does not have a public IP address");
        }

        // Tested: FQDN does not require a trailing dot
        String fqdn = IDUtils.dot(metadata.getGameName(), CommonConfig.ROOT_DOMAIN_NAME.getValue());
        ResourceRecord instanceIpRecord = ResourceRecord.builder().value(publicIp).build();

        return ResourceRecordSet.builder()
                .name(fqdn)
                .type(RRType.A)
                .ttl(CommonConfig.APP_DNS_RECORD_TTL)
                .resourceRecords(instanceIpRecord)
                .build();
    }

    private Instance describeInstance(GameMetadata metadata) {
        DescribeInstancesResponse response = ec2Client.describeInstances(r -> r.instanceIds(metadata.getInstanceId()));
        LogUtils.debugDump(logger, "Dumping instance information:", response);
        if (response.reservations() == null || response.reservations().isEmpty()) {
            throw new IllegalStateException("Could not find any instance from ID supplied in game metadata");
        }
        return response.reservations().get(0).instances().get(0);
    }

}
