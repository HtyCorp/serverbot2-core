package io.mamish.serverbot2.appdaemon;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.strategy.IgnoreErrorContextMissingStrategy;
import io.mamish.serverbot2.framework.exception.ApiException;
import io.mamish.serverbot2.gamemetadata.model.GameMetadata;
import io.mamish.serverbot2.gamemetadata.model.GameReadyState;
import io.mamish.serverbot2.sharedconfig.WorkflowsConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.sfn.SfnClient;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AppDaemon {

    public static void main(String[] args) {
        AWSXRay.getGlobalRecorder().setContextMissingStrategy(new IgnoreErrorContextMissingStrategy());
        new AppDaemon();
    }

    private static final long HEARTBEAT_INTERVAL_SECONDS = (int) (WorkflowsConfig.DAEMON_HEARTBEAT_TIMEOUT_SECONDS / 2.5);

    private final Logger logger = LogManager.getLogger(AppDaemon.class);
    private final SfnClient sfnClient = SfnClient.create();

    public AppDaemon() {
        phoneHome();
        new MessageServer();
        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(this::heartbeat,
                HEARTBEAT_INTERVAL_SECONDS, HEARTBEAT_INTERVAL_SECONDS, TimeUnit.SECONDS);
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

}
