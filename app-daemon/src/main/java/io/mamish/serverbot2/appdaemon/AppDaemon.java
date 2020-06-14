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
import io.mamish.serverbot2.sharedutil.LogUtils;
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
    private static final long IDLENESS_CHECK_INTERVAL_SECONDS = 5 * 60;

    private final String gameName = GameMetadataFetcher.initial().getGameName();
    private final Instant daemonStartTime = Instant.now();
    private boolean idleWarningIssued = false;

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

        Instant now = Instant.now();
        int secondsSinceStart = (int) daemonStartTime.until(now, ChronoUnit.SECONDS);

        if (secondsSinceStart < AppInstanceConfig.APP_IDLENESS_WARNING_TIME_SECONDS) {
            logger.info("Daemon uptime is less than minimum warning time - skipping idleness check");
            return;
        }

        // Now actually get the usage and issue warnings/shutdowns
        // Need to give users at least one warning in all cases: if no warning yet issued, don't do shutdown yet

        if (!idleWarningIssued) {
            if (hasNetworkActivityInWindow(AppInstanceConfig.APP_IDLENESS_WARNING_TIME_SECONDS)) {

                idleWarningIssued = false;

            } else {

                discordServiceClient.newMessage(new NewMessageRequest(
                        gameName + " is idle (no connections) and will shut down soon.",
                        null, MessageChannel.SERVERS, null
                ));
                idleWarningIssued = true;

            }
        } else {
            if (hasNetworkActivityInWindow(AppInstanceConfig.APP_IDLENESS_SHUTDOWN_TIME_SECONDS)) {

                idleWarningIssued = false;

            } else {

                discordServiceClient.newMessage(new NewMessageRequest(
                        gameName + " is idle (no connections) and is shutting down now.",
                        null, MessageChannel.SERVERS, null
                ));

                GameMetadata gameMetadata = GameMetadataFetcher.fetch();
                if (gameMetadata.getGameReadyState() == GameReadyState.RUNNING) {
                    logger.info("Instance is idle and game is in running state - sending task token to shut down");
                    sfnClient.sendTaskSuccess(r -> r.taskToken(gameMetadata.getTaskCompletionToken()).output("{}"));
                } else {
                    logger.error("Instance is idle but game is in unknown status - not attempting Sfn call");
                }

            }
        }

    }

    private boolean hasNetworkActivityInWindow(int timeLimitSeconds) {
        GetNetworkUsageResponse response = networkSecurityServiceClient.getNetworkUsage(new GetNetworkUsageRequest(
                InstanceMetadata.fetch().getPrivateIp(), (int) (timeLimitSeconds * 1.5)
        ));
        LogUtils.debugInfo(logger, "Network activity stats:", response);
        return response.hasAnyActivity() && response.getLatestActivityAgeSeconds() < timeLimitSeconds;
    }

}
