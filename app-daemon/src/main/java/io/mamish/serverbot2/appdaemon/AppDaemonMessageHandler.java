package io.mamish.serverbot2.appdaemon;

import com.google.gson.Gson;
import io.mamish.serverbot2.appdaemon.model.*;
import io.mamish.serverbot2.framework.exception.server.RequestHandlingException;
import io.mamish.serverbot2.framework.server.SqsApiServer;
import io.mamish.serverbot2.sharedconfig.AppInstanceConfig;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AppDaemonMessageHandler extends SqsApiServer<IAppDaemon> implements IAppDaemon {

    private final Gson gson = new Gson();
    private final Logger logger = Logger.getLogger("AppDaemonMessageHandler");
    private final GameMetadataFetcher metadataFetcher;

    private Process runningAppProcess;

    @Override
    protected Class<IAppDaemon> getModelClass() {
        return IAppDaemon.class;
    }

    @Override
    protected IAppDaemon getHandlerInstance() {
        return this;
    }

    public AppDaemonMessageHandler(GameMetadataFetcher metadataFetcher) {
        super(metadataFetcher.initial().getInstanceQueueName());
        this.metadataFetcher = metadataFetcher;
    }

    @Override
    public StartAppResponse startApp(StartAppRequest request) {
        try {
            runProcess();
            return new StartAppResponse();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "IOException in startApp", e);
            throw new RequestHandlingException("IO error during game launch: " + e.getMessage(), e);
        }
    }

    @Override
    public StopAppResponse stopApp(StopAppRequest request) {
        // If it's already null or terminated, consider this a success.
        if (runningAppProcess == null || !runningAppProcess.isAlive()) {
            return new StopAppResponse();
        }

        runningAppProcess.destroy();

        try {
            boolean success = runningAppProcess.waitFor(AppInstanceConfig.APP_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!success) {
                runningAppProcess.destroyForcibly();
                logger.log(Level.WARNING, "stopApp: forced a shutdown after waiting");
            } else {
                logger.log(Level.INFO, "stopApp: clean shutdown");
            }
        } catch (InterruptedException e) {
            runningAppProcess.destroyForcibly();
        }

        return new StopAppResponse();
    }

    private void runProcess() throws IOException {
        Path rootPath = Path.of("/opt", "serverbot2");
        Path gameDir = rootPath.resolve("game");
        Path configDir = rootPath.resolve("config");

        FileReader gameConfigFile = new FileReader(configDir.resolve("game.cfg").toFile());
        GameConfigFile config = gson.fromJson(gameConfigFile, GameConfigFile.class);

        Instant sessionStart = Instant.now();

        runningAppProcess = new ProcessBuilder()
                .directory(gameDir.toFile())
                .command(config.getLaunchCmd())
                .start();

        new CloudWatchLogsUploader(runningAppProcess.getInputStream(), metadataFetcher.initial().getGameName(),
                sessionStart, "stdout");
        new CloudWatchLogsUploader(runningAppProcess.getErrorStream(), metadataFetcher.initial().getGameName(),
                sessionStart, "stderr");

    }

}
