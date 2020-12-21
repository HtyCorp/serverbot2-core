package com.admiralbot.workflows;

import com.admiralbot.sharedconfig.CommonConfig;
import com.admiralbot.sharedutil.LogUtils;
import com.admiralbot.sharedutil.SdkUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.services.route53.Route53Client;
import software.amazon.awssdk.services.route53.model.*;

public class AppDnsRecordManager {

    private static final String HOSTED_ZONE_ID = CommonConfig.APP_ROOT_DOMAIN_ZONE_ID.getValue();

    private final Route53Client route53Client = SdkUtils.globalClient(Route53Client.builder());

    private final Logger logger = LogManager.getLogger(AppDnsRecordManager.class);

    public String getLocationString(String name) {
        ResourceRecordSet recordSet = lookupResourceRecord(name);
        String fqdn = recordSet.name();
        String fqdnNoTrailingDot = fqdn.substring(0, fqdn.length()-1);
        String address = recordSet.resourceRecords().get(0).value();
        return "'" + fqdnNoTrailingDot + "' (" + address + ")";
    }

    public void updateAppRecord(String name, String address) {
        changeRecordForApp(name, address, ChangeAction.UPSERT);
    }

    public void deleteAppRecord(String name) {
        String address = lookupResourceRecord(name).resourceRecords().get(0).value();
        changeRecordForApp(name, address, ChangeAction.DELETE);
    }

    private void changeRecordForApp(String name, String address, ChangeAction action) {
        ChangeBatch changeBatch = buildChangeBatch(name, address, action);
        LogUtils.debugDump(logger, "Dumping pending resource " + action + " change batch:", changeBatch);
        route53Client.changeResourceRecordSets(r -> r.hostedZoneId(HOSTED_ZONE_ID).changeBatch(changeBatch));
    }

    private ChangeBatch buildChangeBatch(String name, String address, ChangeAction action) {
        ResourceRecordSet recordSet = buildRecordSet(name, address);
        Change change = Change.builder()
                .action(action)
                .resourceRecordSet(recordSet)
                .build();
        return ChangeBatch.builder()
                .changes(change)
                .build();
    }

    private ResourceRecordSet buildRecordSet(String name, String address) {
        ResourceRecord instanceIpRecord = ResourceRecord.builder().value(address).build();

        return ResourceRecordSet.builder()
                .name(makeFqdn(name))
                .type(RRType.A)
                .ttl(CommonConfig.APP_DNS_RECORD_TTL)
                .resourceRecords(instanceIpRecord)
                .build();
    }

    private ResourceRecordSet lookupResourceRecord(String name) {
        return route53Client.listResourceRecordSets(r -> r.hostedZoneId(HOSTED_ZONE_ID)
                .startRecordType(RRType.A)
                .startRecordName(makeFqdn(name))
                .maxItems("1")
        ).resourceRecordSets().get(0);
    }

    private String makeFqdn(String appName) {
        return appName + "." + CommonConfig.APP_ROOT_DOMAIN_NAME.getValue() + ".";
    }

}
