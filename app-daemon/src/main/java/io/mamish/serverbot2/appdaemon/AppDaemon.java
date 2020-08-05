package io.mamish.serverbot2.appdaemon;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.strategy.IgnoreErrorContextMissingStrategy;
import io.mamish.serverbot2.discordrelay.model.service.IDiscordService;
import io.mamish.serverbot2.discordrelay.model.service.MessageChannel;
import io.mamish.serverbot2.discordrelay.model.service.NewMessageRequest;
import io.mamish.serverbot2.framework.client.ApiClient;
import io.mamish.serverbot2.framework.exception.ApiException;
import io.mamish.serverbot2.gamemetadata.model.GameMetadata;
import io.mamish.serverbot2.gamemetadata.model.GameReadyState;
import io.mamish.serverbot2.networksecurity.model.GetNetworkUsageRequest;
import io.mamish.serverbot2.networksecurity.model.GetNetworkUsageResponse;
import io.mamish.serverbot2.networksecurity.model.INetworkSecurity;
import io.mamish.serverbot2.sharedconfig.AppInstanceConfig;
import io.mamish.serverbot2.sharedconfig.DiscordConfig;
import io.mamish.serverbot2.sharedconfig.NetSecConfig;
import io.mamish.serverbot2.sharedconfig.WorkflowsConfig;
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
        AWSXRay.getGlobalRecorder().setContextMissingStrategy(new IgnoreErrorContextMissingStrategy());
        new AppDaemon();
    }

    private static final long HEARTBEAT_INTERVAL_SECONDS = (int) (WorkflowsConfig.DAEMON_HEARTBEAT_TIMEOUT_SECONDS / 2.5);

    // Temporarily extended to 15-min check due to IP re-auth issue: if connected user re-auths, netsec:GetNetworkUsage
    // CWL scan no longer matches VPCFL entries from the previous IP address when getting "latest activity" time.
    private static final long IDLENESS_CHECK_INTERVAL_SECONDS = 15 * 60;

    private final String gameName = GameMetadataFetcher.initial().getGameName();
    private final Instant daemonStartTime = Instant.now();
    private IdleState idleState = IdleState.ACTIVE_USE;

    private final Logger logger = LogManager.getLogger(AppDaemon.class);
    private final SfnClient sfnClient = SfnClient.create();
    private final INetworkSecurity networkSecurityServiceClient = ApiClient.lambda(INetworkSecurity.class,
            NetSecConfig.FUNCTION_NAME);
    private final IDiscordService discordServiceClient = ApiClient.sqs(IDiscordService.class,
            DiscordConfig.SQS_QUEUE_NAME);

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

        // Short circuit: don't bother checking if instance was too recently started to breach time limit
        int secondsSinceStart = (int) daemonStartTime.until(Instant.now(), ChronoUnit.SECONDS);
        if (secondsSinceStart < AppInstanceConfig.APP_IDLENESS_WARNING_TIME_SECONDS) {
            logger.info("Daemon uptime is less than minimum warning time - skipping idleness check");
            return;
        }

        // Now actually get the usage and issue warnings/shutdowns

        GetNetworkUsageResponse usage = networkSecurityServiceClient.getNetworkUsage(new GetNetworkUsageRequest(
                InstanceMetadata.fetch().getPrivateIp(), AppInstanceConfig.APP_IDLENESS_SHUTDOWN_TIME_SECONDS
        ));
        if (usage.hasAnyActivity()) {
            logger.info("Idle check: last activity was " + usage.getLatestActivityAgeSeconds() + " seconds ago");
        } else {
            logger.info("Idle check: no recent usage within shutdown window");
        }

        // Internal state machine: users always get at least one warning before shutdown is committed
        // States: ACTIVE_USE <---> WARNING_ISSUED ----> SHUTDOWN_STARTED

        switch(idleState) {
            case ACTIVE_USE:
                if (!hasRecentActivity(usage, AppInstanceConfig.APP_IDLENESS_WARNING_TIME_SECONDS)) {
                    logger.info("Idle check: issuing idleness warning");
                    idleState = IdleState.WARNING_ISSUED;
                    sendIdlenessWarning();
                }
                break;

            case WARNING_ISSUED:
                if (hasRecentActivity(usage, AppInstanceConfig.APP_IDLENESS_WARNING_TIME_SECONDS)) {
                    logger.info("Idle check: new recent activity, leaving warning state");
                    idleState = IdleState.ACTIVE_USE;
                } else if (!hasRecentActivity(usage, AppInstanceConfig.APP_IDLENESS_SHUTDOWN_TIME_SECONDS)) {
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

    private boolean hasRecentActivity(GetNetworkUsageResponse usage, int timeSeconds) {
        return usage.hasAnyActivity() && usage.getLatestActivityAgeSeconds() < timeSeconds;
    }

    private void sendIdlenessWarning() {
        discordServiceClient.newMessage(new NewMessageRequest(
                gameName + " is idle (no connections) and will shut down soon.",
                null, MessageChannel.SERVERS, null
        ));
    }

    private void sendIdleShutdownNotice() {
        discordServiceClient.newMessage(new NewMessageRequest(
                gameName + " is idle and shutting down now.",
                null, MessageChannel.SERVERS, null
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
