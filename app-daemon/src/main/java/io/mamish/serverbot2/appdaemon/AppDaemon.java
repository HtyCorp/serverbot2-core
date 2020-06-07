package io.mamish.serverbot2.appdaemon;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.strategy.IgnoreErrorContextMissingStrategy;
import io.mamish.serverbot2.gamemetadata.model.GameReadyState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.services.sfn.SfnClient;

public class AppDaemon {

    private static final Logger logger = LogManager.getLogger(AppDaemon.class);

    private static final SfnClient sfnClient = SfnClient.create();

    public static void main(String[] args) {
        AWSXRay.getGlobalRecorder().setContextMissingStrategy(new IgnoreErrorContextMissingStrategy());
        phoneHome();
        new MessageServer();
    }

    private static void phoneHome() {
        GameReadyState state = GameMetadataFetcher.cached().getGameReadyState();
        String taskToken = GameMetadataFetcher.cached().getTaskCompletionToken();

        if (state != GameReadyState.STARTING) {
            logger.info("No task callback made: game isn't in STARTING state");
        } else {
            logger.debug("Sending Sfn task completion to end wait for instance startup");
            sfnClient.sendTaskSuccess(r -> r.taskToken(taskToken).output("{}"));
        }
    }

}
