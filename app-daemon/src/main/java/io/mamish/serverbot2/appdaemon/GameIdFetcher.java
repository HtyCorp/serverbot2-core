package io.mamish.serverbot2.appdaemon;

import io.mamish.serverbot2.sharedconfig.AppInstanceConfig;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.Tag;

import java.util.Optional;

public class GameIdFetcher {

    private final Ec2Client ec2Client = Ec2Client.create();

    public String fetchId() {
        InstanceMetadata metadata = InstanceMetadata.fetch();
        Instance thisInstance = ec2Client.describeInstances(r -> r.instanceIds(metadata.getInstanceId()))
                .reservations().get(0).instances().get(0);
        Optional<String> tagValue = thisInstance.tags().stream()
                .filter(t -> t.key().equals(AppInstanceConfig.APP_NAME_INSTANCE_TAG_KEY))
                .findFirst()
                .map(Tag::value);

        if (tagValue.isEmpty()) {
            throw new IllegalStateException("Game ID tag is missing from instance");
        }
        return tagValue.get();
    }

}
