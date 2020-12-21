package com.admiralbot.resourcereaper;

import com.admiralbot.framework.client.ApiClient;
import com.admiralbot.networksecurity.model.INetworkSecurity;
import com.admiralbot.networksecurity.model.RevokeExpiredIpsRequest;
import com.admiralbot.sharedconfig.ApiConfig;
import com.admiralbot.sharedconfig.ReaperConfig;
import com.admiralbot.sharedutil.AppContext;
import com.admiralbot.sharedutil.SdkUtils;
import com.admiralbot.sharedutil.XrayUtils;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.time.Instant;

public class ScheduledLambdaHandler implements RequestHandler<ScheduledEvent,String> {

    static {
        XrayUtils.setServiceName("ResourceReaper");
        AppContext.setLambda();
    }

    private static final String TAGKEY = ReaperConfig.HEARTBEAT_TAG_NAME;

    private final SqsClient sqs = SdkUtils.client(SqsClient.builder());
    private final INetworkSecurity networkSecurityServiceClient = ApiClient.http(INetworkSecurity.class);

    @Override
    public String handleRequest(ScheduledEvent scheduledEvent, Context context) {
        long epochTime = Instant.now().getEpochSecond();

        cleanUpTempApiSqsQueues(epochTime);
        invokeNetSecExpiredIpsRevoke();

        return "Done!";
    }

    // Hopefully won't ever be important, but ListQueues can't return more than 1000 queues (static API-level limitation).
    private void cleanUpTempApiSqsQueues(long epochTime) {
        long ttl = ReaperConfig.TTL_SECONDS_API_TEMP_SQS_QUEUE;
        sqs.listQueues(r -> r.queueNamePrefix(ApiConfig.TEMP_QUEUE_URL_PREFIX)).queueUrls().stream()
                .filter(url -> {
                    try {
                        String tagValue = sqs.listQueueTags(r -> r.queueUrl(url)).tags().get(TAGKEY);
                        if (tagValue == null) {
                            return true;
                        } else {
                            long secondsSinceHeartbeat = epochTime - Long.parseLong(tagValue);
                            return secondsSinceHeartbeat > ttl;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        return false;
                    }
                })
                .forEach(url -> sqs.deleteQueue(r -> r.queueUrl(url)));
    }

    private void invokeNetSecExpiredIpsRevoke() {
        networkSecurityServiceClient.revokeExpiredIps(new RevokeExpiredIpsRequest());
    }

}
