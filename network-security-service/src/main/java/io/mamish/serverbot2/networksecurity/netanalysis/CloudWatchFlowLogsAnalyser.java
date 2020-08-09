package io.mamish.serverbot2.networksecurity.netanalysis;

import com.amazonaws.xray.AWSXRay;
import io.mamish.serverbot2.framework.exception.server.RequestHandlingException;
import io.mamish.serverbot2.networksecurity.model.GetNetworkUsageResponse;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedutil.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.GetQueryResultsResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.QueryStatus;
import software.amazon.awssdk.services.cloudwatchlogs.model.ResultField;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

public class CloudWatchFlowLogsAnalyser implements INetworkAnalyser {

    private static final int PLACEHOLDER_AGE_FOR_NO_ACTIVITY = Integer.MAX_VALUE / 2;
    private static final long QUERY_CHECK_INTERVAL_MILLIS = 2000;

    private final CloudWatchLogsClient logsClient = CloudWatchLogsClient.builder()
            .httpClient(UrlConnectionHttpClient.create())
            .build();

    private final Logger logger = LogManager.getLogger(CloudWatchFlowLogsAnalyser.class);

    @Override
    public GetNetworkUsageResponse analyse(List<String> authorisedIps, String endpointVpcIp, int windowSeconds) {

        String queryString = buildQueryString(authorisedIps, endpointVpcIp);
        logger.debug("Query string for flow logs analysis is:\n" + queryString);

        Instant now = Instant.now();
        long endTime = now.toEpochMilli();
        long startTime = now.minus(windowSeconds, ChronoUnit.SECONDS).toEpochMilli();

        String queryId = logsClient.startQuery(r -> r.logGroupName(CommonConfig.APPLICATION_VPC_FLOW_LOGS_GROUP_NAME)
                .startTime(startTime)
                .endTime(endTime)
                .queryString(queryString)
                .build()
        ).queryId();

        try {
            AWSXRay.beginSubsegment("PollQueryResult");

            List<List<ResultField>> results = null;
            QueryStatus status = QueryStatus.SCHEDULED;
            while (Utils.equalsAny(status, QueryStatus.SCHEDULED, QueryStatus.RUNNING)) {
                try {
                    Thread.sleep(QUERY_CHECK_INTERVAL_MILLIS);
                } catch (InterruptedException e) {
                    logger.error("Unexpected thread interrupt while polling for query result", e);
                }

                GetQueryResultsResponse response = logsClient.getQueryResults(r -> r.queryId(queryId));
                status = response.status();
                results = response.results();
            }

            if (status != QueryStatus.COMPLETE) {
                throw new RequestHandlingException("Non-success status (" + status + ") from Insights query");
            }

            if (results == null || results.isEmpty()) {
                logger.info("Empty results: returning a 'no-activity' final result");
                return new GetNetworkUsageResponse(false, PLACEHOLDER_AGE_FOR_NO_ACTIVITY);
            }

            // Get first result field of first result row (we know there's a field because it's part of query string)
            ResultField timestampResult = results.get(0).get(0);
            if (!timestampResult.field().equals("latestTimeUnix")) {
                throw new RequestHandlingException("Unexpected field names in query response");
            }
            long latestTimeUnix = Long.parseLong(timestampResult.value());
            int latestActivityAgeSeconds = (int) Instant.ofEpochMilli(latestTimeUnix).until(now, ChronoUnit.SECONDS);

            return new GetNetworkUsageResponse(true, latestActivityAgeSeconds);

        } catch (RuntimeException e) {
            AWSXRay.getTraceEntity().addException(e);
            throw e;
        } finally {
            AWSXRay.endSubsegment();
        }
    }

    private String buildQueryString(List<String> authorisedIps, String endpointVpcIp) {
        // Available fields:
        // @timestamp, @logStream, @message, accountId, endTime, interfaceId, logStatus, startTime, version, action,
        // bytes, dstAddr, dstPort, packets, protocol, srcAddr, srcPort
        String ipArrayString = authorisedIps.stream()
                .map(ip -> "\"" + ip + "\"")
                .collect(Collectors.joining(",","[","]"));
        return "fields @timestamp, action, srcAddr, dstAddr"
                + " | filter (action=\"ACCEPT\" and dstAddr=\"" + endpointVpcIp + "\" and srcAddr in " + ipArrayString + ")"
                + " | stats latest(@timestamp) as latestTimeUnix";
    }

}
