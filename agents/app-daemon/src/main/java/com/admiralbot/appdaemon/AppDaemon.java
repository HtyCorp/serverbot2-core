package com.admiralbot.appdaemon;

import com.admiralbot.discordrelay.model.service.IDiscordService;
import com.admiralbot.discordrelay.model.service.MessageChannel;
import com.admiralbot.discordrelay.model.service.NewMessageRequest;
import com.admiralbot.framework.client.ApiClient;
import com.admiralbot.framework.exception.ApiException;
import com.admiralbot.gamemetadata.model.GameMetadata;
import com.admiralbot.gamemetadata.model.GameReadyState;
import com.admiralbot.networksecurity.model.GetNetworkUsageRequest;
import com.admiralbot.networksecurity.model.GetNetworkUsageResponse;
import com.admiralbot.networksecurity.model.INetworkSecurity;
import com.admiralbot.sharedconfig.AppInstanceConfig;
import com.admiralbot.sharedconfig.WorkflowsConfig;
import com.admiralbot.sharedutil.AppContext;
import com.admiralbot.sharedutil.SdkUtils;
import com.admiralbot.sharedutil.XrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.sfn.SfnClient;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AppDaemon {

    public static void main(String[] args) {
        XrayUtils.setIgnoreMissingContext();
        XrayUtils.setServiceName("AppDaemon");
        AppContext.setInstance();
        new AppDaemon();
    }

    private static final long HEARTBEAT_INTERVAL_SECONDS = (int) (WorkflowsConfig.DAEMON_HEARTBEAT_TIMEOUT_SECONDS / 2.5);
    private static final long IDLENESS_CHECK_INTERVAL_SECONDS = 2 * 60;

    private static final int WARNING_TIME_SECONDS = AppInstanceConfig.APP_IDLENESS_WARNING_TIME_SECONDS;
    private static final int SHUTDOWN_TIME_SECONDS = AppInstanceConfig.APP_IDLENESS_SHUTDOWN_TIME_SECONDS;

    private final String gameName = GameMetadataFetcher.initial().getGameName();
    private Instant latestNetworkActivityTime = Instant.now();
    private IdleState idleState = IdleState.ACTIVE_USE;

    private final Logger logger = LogManager.getLogger(AppDaemon.class);
    private final SfnClient sfnClient = SdkUtils.client(SfnClient.builder());
    private final INetworkSecurity networkSecurityServiceClient = ApiClient.http(INetworkSecurity.class);
    private final IDiscordService discordServiceClient = ApiClient.http(IDiscordService.class);

    public AppDaemon() {
        phoneHome();
        new MessageServer();
        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(this::heartbeat,
                HEARTBEAT_INTERVAL_SECONDS, HEARTBEAT_INTERVAL_SECONDS, TimeUnit.SECONDS);
        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(this::checkIdleness,
                IDLENESS_CHECK_INTERVAL_SECONDS, IDLENESS_CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private void phoneHome() {
        GameReadyState state = GameMetadataFetcher.initial().getGameReadyState();
        String taskToken = GameMetadataFetcher.initial().getTaskCompletionToken();

        if (state == GameReadyState.STARTING) {
            logger.debug("Sending Sfn task completion to end wait for instance startup");
            sfnClient.sendTaskSuccess(r -> r.taskToken(taskToken).output("{}"));
        } else {
            logger.info("No task callback made: game isn't in STARTING state");
        }
    }

    private void heartbeat() {
        try {
            GameMetadata metadata = GameMetadataFetcher.fetch();
            if (metadata.getGameReadyState() == GameReadyState.RUNNING) {
                logger.debug("Sending Sfn heartbeat");
                sfnClient.sendTaskHeartbeat(r -> r.taskToken(metadata.getTaskCompletionToken()));
            } else {
                logger.debug("GMS game state is " + metadata.getGameReadyState() + ": not sending heartbeat");
            }
        } catch (SdkException e) {
            logger.error("AWS API error in Sfn heartbeat for running daemon", e);
        } catch (ApiException e) {
            logger.error("GMS API error in Sfn heartbeat for running daemon", e);
        }
    }

    private void checkIdleness() {
        try {
            doCheckIdleness();
        } catch (Exception e) {
            // Log exception but otherwise ignore it - prevents further scheduled executions being suppressed
            logger.error("Uncaught exception in idle checker", e);
        }
    }

    private void doCheckIdleness() {

        Instant now = Instant.now();

        // Short circuit: don't proceed (avoid API calls) if locally known activity age is less than warning time
        int secondsSinceLatest = (int) latestNetworkActivityTime.until(now, ChronoUnit.SECONDS);
        if (secondsSinceLatest < WARNING_TIME_SECONDS) {
            logger.info("Skipping idleness check - local known latest is still within warning period");
            return;
        }

        // Get actual latest activity time from NetSec: update locally known latest time if more recent.
        // Note this ignores the response data if it's *older* than local; can happen if a user re-auths since
        // that overwrites the old IP and GetNetworkUsage only counts activity from currently whitelisted IPs.
        GetNetworkUsageResponse response = networkSecurityServiceClient.getNetworkUsage(new GetNetworkUsageRequest(
                InstanceMetadata.fetch().getPrivateIp(),
                GameMetadataFetcher.initial().getGameName(), // app/game name matches security group name
                SHUTDOWN_TIME_SECONDS
        ));
        logger.info("Idle check: NetSec latest activity response: hasActivity=" + response.hasAnyActivity()
                + ", age=" + response.getLatestActivityAgeSeconds());

        if (response.hasAnyActivity() && response.getLatestActivityAgeSeconds() < secondsSinceLatest) {
            secondsSinceLatest = response.getLatestActivityAgeSeconds();
            latestNetworkActivityTime = now.minus(secondsSinceLatest, ChronoUnit.SECONDS);
            logger.info("Idle check: NetSec is reporting more recent activity - updating local data");
        } else {
            logger.info("Idle check: NetSec reported activity isn't newer than local - ignoring");
        }

        // Internal state machine: users always get at least one warning before shutdown is committed
        // States: ACTIVE_USE <---> WARNING_ISSUED ----> SHUTDOWN_STARTED

        switch(idleState) {
            case ACTIVE_USE:
                if (secondsSinceLatest >= WARNING_TIME_SECONDS) {
                    logger.info("Idle check: issuing idleness warning");
                    idleState = IdleState.WARNING_ISSUED;
                    // On transition, set local latest time to now minus the warning time.
                    // This guarantees users have at least (shutdown_time - warning_time) until shutdown happens.
                    latestNetworkActivityTime = now.minus(WARNING_TIME_SECONDS, ChronoUnit.SECONDS);
                    sendIdlenessWarning();
                }
                break;

            case WARNING_ISSUED:
                if (secondsSinceLatest < WARNING_TIME_SECONDS) {
                    logger.info("Idle check: new recent activity, leaving warning state");
                    idleState = IdleState.ACTIVE_USE;
                } else if (secondsSinceLatest >= SHUTDOWN_TIME_SECONDS) {
                    logger.info("Idle check: shutdown window breached, signalling shutdown");
                    idleState = IdleState.SHUTDOWN_STARTED;
                    sendIdleShutdownNotice();
                    signalShutdownToSfn();
                }
                break;

            case SHUTDOWN_STARTED:
                logger.info("Idle check: still in shutdown state, nothing to do");
                break;

        }

    }

    private void sendIdlenessWarning() {
        discordServiceClient.newMessage(new NewMessageRequest(
                gameName + " is idle (no connections) and will shut down soon.",
                null, MessageChannel.MAIN, null
        ));
    }

    private void sendIdleShutdownNotice() {
        discordServiceClient.newMessage(new NewMessageRequest(
                gameName + " is idle and shutting down now.",
                null, MessageChannel.MAIN, null
        ));
    }

    private void signalShutdownToSfn() {
        GameMetadata gameMetadata = GameMetadataFetcher.fetch();
        if (gameMetadata.getGameReadyState() == GameReadyState.RUNNING) {
            logger.info("Instance is idle and game is in running state - sending task token to shut down");
            sfnClient.sendTaskSuccess(r -> r.taskToken(gameMetadata.getTaskCompletionToken()).output("{}"));
        } else {
            logger.error("Instance is idle but game is in unknown status - not attempting Sfn call");
        }
    }

}
