package com.admiralbot.networksecurity.netanalysis;

import com.admiralbot.framework.exception.server.RequestHandlingException;
import com.admiralbot.networksecurity.model.PortPermission;
import com.admiralbot.networksecurity.model.PortProtocol;
import com.admiralbot.sharedconfig.CommonConfig;
import com.admiralbot.sharedutil.SdkUtils;
import com.admiralbot.sharedutil.Utils;
import com.amazonaws.xray.AWSXRay;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.GetQueryResultsResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.QueryStatus;
import software.amazon.awssdk.services.cloudwatchlogs.model.ResultField;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CloudWatchFlowLogsAnalyser implements INetworkAnalyser {

    private static final long QUERY_CHECK_INTERVAL_MILLIS = 2000;

    private final CloudWatchLogsClient logsClient = SdkUtils.client(CloudWatchLogsClient.builder());

    private final Logger logger = LoggerFactory.getLogger(CloudWatchFlowLogsAnalyser.class);

    @Override
    public Optional<Integer> getLatestActivityAgeSeconds(List<String> authorisedIps, List<PortPermission> authorisedPorts,
                                                         String endpointVpcIp, int windowSeconds) {

        if (authorisedIps.isEmpty()) {
            throw new IllegalArgumentException("Can't construct a valid query with no authorised IPs");
        }
        if (authorisedPorts.isEmpty()) {
            throw new IllegalArgumentException("Can't construct a valid query with no authorised ports");
        }

        String queryString = buildQueryString(authorisedIps, authorisedPorts, endpointVpcIp);
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

                    //noinspection BusyWait
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
                logger.info("Log query result is empty: return null activity age");
                return Optional.empty();
            }

            // Get first result field of first result row (we know there's a field because it's part of query string)
            ResultField timestampResult = results.get(0).get(0);
            if (!timestampResult.field().equals("latestTimeUnix")) {
                throw new RequestHandlingException("Unexpected field names in query response");
            }
            long latestTimeUnix = Long.parseLong(timestampResult.value());
            int latestActivityAgeSeconds = (int) Instant.ofEpochMilli(latestTimeUnix).until(now, ChronoUnit.SECONDS);

            logger.info("Log query result got an activity age of {} seconds", latestActivityAgeSeconds);
            return Optional.of(latestActivityAgeSeconds);

        } catch (RuntimeException e) {
            AWSXRay.getTraceEntity().addException(e);
            throw e;
        } finally {
            AWSXRay.endSubsegment();
        }
    }

    private String buildQueryString(List<String> authorisedIps, List<PortPermission> authorisedPorts, String endpointVpcIp) {
        // Available fields:
        // @timestamp, @logStream, @message, accountId, endTime, interfaceId, logStatus, startTime, version, action,
        // bytes, dstAddr, dstPort, packets, protocol, srcAddr, srcPort

        String destinationConditionString = "(action=\"ACCEPT\" and dstAddr=\"" + endpointVpcIp + "\")";
        String ipConditionString = "(srcAddr in " + authorisedIps.stream()
                .map(ip -> "\"" + ip + "\"")
                .collect(Collectors.joining(",", "[", "])"));
        String portConditionString = authorisedPorts.stream()
                .map(this::buildPortCondition)
                .collect(Collectors.joining(" or ", "(", ")"));

        return "filter "+destinationConditionString+" and " +ipConditionString+" and "+portConditionString+
                " | stats latest(@timestamp) as latestTimeUnix";
    }

    private String buildPortCondition(PortPermission portPermission) {
        return String.format("(protocol = %d and dstPort >= %d and dstPort <= %d)",
                PortProtocol.toProtocolNumber(portPermission.getProtocol()),
                portPermission.getPortRangeFrom(),
                portPermission.getPortRangeTo());
    }

}
