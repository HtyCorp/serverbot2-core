package io.mamish.serverbot2.appdaemon;

import com.google.gson.Gson;
import io.mamish.serverbot2.appdaemon.model.*;
import io.mamish.serverbot2.framework.exception.server.RequestHandlingException;
import io.mamish.serverbot2.framework.exception.server.RequestValidationException;
import io.mamish.serverbot2.sharedconfig.AppInstanceConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class ServiceHandler implements IAppDaemon {

    private final Gson gson = new Gson();
    private final Logger logger = LogManager.getLogger(ServiceHandler.class);

    private Process runningAppProcess;

    @Override
    public StartAppResponse startApp(StartAppRequest request) {

        if (runningAppProcess != null && runningAppProcess.isAlive()) {
            logger.error("Received StartApp call with instance already running");
            throw new RequestValidationException("Process already running");
        }

        try {
            runProcess();
            return new StartAppResponse();
        } catch (IOException e) {
            logger.error("IOException in startApp", e);
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
                logger.warn("stopApp: forced a shutdown after waiting");
            } else {
                logger.info("stopApp: clean shutdown");
            }
        } catch (InterruptedException e) {
            runningAppProcess.destroyForcibly();
        }

        return new StopAppResponse();
    }

    private void runProcess() throws IOException {

        logger.debug("runProcess: resolving file paths");

        Path rootPath = Path.of("/opt", "serverbot2");
        Path gameDir = rootPath.resolve("game");
        Path configDir = rootPath.resolve("config");

        logger.debug("runProcess: parsing game config file");

        FileReader gameConfigFile = new FileReader(configDir.resolve("game.cfg").toFile());
        GameConfigFile config = gson.fromJson(gameConfigFile, GameConfigFile.class);

        if (config.getLaunchCommand() == null || config.getLaunchCommand().isEmpty()) {
            logger.error("Missing launch command in game config file");
            throw new RequestHandlingException("Missing launch command in game config file");
        }

        logger.debug("runProcess: building process");
        ProcessBuilder processBuilder = new ProcessBuilder()
                .directory(gameDir.toFile())
                .command(config.getLaunchCommand());

        if (config.getEnvironment() == null || config.getEnvironment().isEmpty()) {
            logger.info("runProcess: no environment specified, using unmodified inherited env");
        } else {
            logger.info(() -> "runProcess: inserting requested env values:\n" + gson.toJson(config.getEnvironment()));
            processBuilder.environment().putAll(config.getEnvironment());
        }

        logger.info("runProcess: starting process");
        Instant sessionStartTime = Instant.now();
        runningAppProcess = processBuilder.start();

        new CloudWatchLogsUploader(runningAppProcess.getInputStream(), GameMetadataFetcher.initial().getGameName(),
                sessionStartTime, "stdout");
        new CloudWatchLogsUploader(runningAppProcess.getErrorStream(), GameMetadataFetcher.initial().getGameName(),
                sessionStartTime, "stderr");

    }

}