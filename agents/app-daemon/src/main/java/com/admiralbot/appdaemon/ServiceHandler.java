package com.admiralbot.appdaemon;

import com.admiralbot.appdaemon.model.*;
import com.admiralbot.framework.exception.server.RequestHandlingException;
import com.admiralbot.framework.exception.server.RequestValidationException;
import com.admiralbot.sharedconfig.AppInstanceConfig;
import com.admiralbot.sharedconfig.CommonConfig;
import com.admiralbot.sharedutil.Joiner;
import com.admiralbot.sharedutil.LogUtils;
import com.admiralbot.sharedutil.Pair;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import software.amazon.awssdk.core.SdkBytes;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class ServiceHandler implements IAppDaemon {

    private final Gson gson = new Gson();
    private final Logger logger = LoggerFactory.getLogger(ServiceHandler.class);

    private Process runningAppProcess;
    private SessionSftpServer sessionSftpServer;

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

        FileReader gameConfigFile = new FileReader(configDir.resolve("launch.cfg").toFile());
        GameConfigFile config = gson.fromJson(gameConfigFile, GameConfigFile.class);

        if (config.getLaunchCommand() == null || config.getLaunchCommand().isEmpty()) {
            logger.error("Missing launch command in game config file");
            throw new RequestHandlingException("Missing launch command in game config file");
        }

        List<String> modifiedCommand = config.getLaunchCommand();
        if (config.isRelativePath()) {
            logger.debug("Launch config is a relative path: generating absolute path for process builder");
            String absoluteCommand = gameDir.resolve(modifiedCommand.get(0)).toString();
            modifiedCommand.set(0, absoluteCommand);
            logger.debug("Replaced command with absolute variant '" + absoluteCommand + "'");
        }

        logger.debug("runProcess: building process");
        ProcessBuilder processBuilder = new ProcessBuilder()
                .directory(gameDir.toFile())
                .command(modifiedCommand);

        if (config.getEnvironment() == null || config.getEnvironment().isEmpty()) {
            logger.info("runProcess: no environment specified, using unmodified inherited env");
        } else {
            LogUtils.infoDump(logger, "runProcess: inserting requested env values:", config.getEnvironment());
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

    @Override
    public StartSftpServerResponse startSftpServer(StartSftpServerRequest request) {
        if (sessionSftpServer == null) {
            try {
                sessionSftpServer = new SessionSftpServer();
            } catch (IOException e) {
                logger.error("IOException while initialising SFTP server", e);
                throw new RequestHandlingException("Unable to start SFTP on server");
            }
        }

        return new StartSftpServerResponse(new SftpSession(
                sessionSftpServer.getSessionUsername(),
                sessionSftpServer.getSessionPassword(),
                sessionSftpServer.getSessionKeyPairFingerprint()
        ));
    }

    @Override
    public ExtendDiskResponse extendDisk(ExtendDiskRequest request) {

        /* Example `lsblk --json` output from Ubuntu 18.04 LTS on m5.large, with added EBS volume (9G):
         * {
         *     "blockdevices": [
         *         {"name": "loop0", "maj:min": "7:0", "rm": "0", "size": "97.8M", "ro": "1", "type": "loop", "mountpoint": "/snap/core/10185"},
         *         {"name": "loop1", "maj:min": "7:1", "rm": "0", "size": "28.1M", "ro": "1", "type": "loop", "mountpoint": "/snap/amazon-ssm-agent/2012"},
         *         {"name": "loop2", "maj:min": "7:2", "rm": "0", "size": "240K", "ro": "1", "type": "loop", "mountpoint": "/snap/jq/6"},
         *         {"name": "nvme1n1", "maj:min": "259:0", "rm": "0", "size": "9G", "ro": "0", "type": "disk", "mountpoint": null},
         *         {"name": "nvme0n1", "maj:min": "259:1", "rm": "0", "size": "8G", "ro": "0", "type": "disk", "mountpoint": null,
         *            "children": [
         *               {"name": "nvme0n1p1", "maj:min": "259:2", "rm": "0", "size": "8G", "ro": "0", "type": "part", "mountpoint": "/"}
         *            ]
         *         }
         *      ]
         *   }
         */

        String lsblkJson = runBashCommand("lsblk --json", 2);
        LsblkOutput lsblk = gson.fromJson(lsblkJson, LsblkOutput.class);
        Pair<String,String> rootDeviceAndPartition = lsblk.blockDevices.stream()
                // Only get root devices with partitions
                .filter(device -> device.children != null
                        && CommonConfig.EBS_ROOT_DEVICE_NAMES_NO_DEV_PREFIX.contains(device.name))
                // Get the partition mounted in filesystem root ("/")
                .map(device -> {
                    LsblkBlockDevice rootPartition = device.children.stream()
                            .filter(child -> "/".equals(child.mountpoint))
                            .findFirst().orElseThrow();
                    return new Pair<>(device.name, rootPartition.name);
                }).findFirst().orElseThrow();

        String rootDeviceName = rootDeviceAndPartition.a();
        String rootPartitionName = rootDeviceAndPartition.b();
        int rootPartitionNumber = Integer.parseInt(getLastChar(rootPartitionName));

        String growPartitionCommand = Joiner.space(
                "sudo", "growpart", "/dev/"+rootDeviceName, rootPartitionNumber
        );
        String resizeFilesystemCommand = Joiner.space(
                "sudo", "resize2fs", "/dev/"+rootPartitionName
        );
        runBashCommand(growPartitionCommand, 3);
        runBashCommand(resizeFilesystemCommand, 3);

        // Do lsblk again to get actual expanded partition size - if there were other partitions this won't actually be
        // all of the space on the disk.

        String modifiedLsblkJson = runBashCommand("lsblk --json", 2);
        LsblkOutput modifiedLsblk = gson.fromJson(modifiedLsblkJson, LsblkOutput.class);
        LsblkBlockDevice modifiedRootPartition = modifiedLsblk.blockDevices.stream()
                .filter(device -> device.children != null)
                .flatMap(device -> device.children.stream())
                .filter(child -> rootPartitionName.equals(child.name))
                .findFirst().orElseThrow();

        return new ExtendDiskResponse(modifiedRootPartition.name, modifiedRootPartition.size);

    }

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private static class LsblkOutput {
        @SerializedName("blockdevices")
        private List<LsblkBlockDevice> blockDevices;
    }

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private static class LsblkBlockDevice {
        String name;
        @SerializedName("maj:min")
        String version;
        String rm, size, ro, type, mountpoint;
        List<LsblkBlockDevice> children;
    }

    private String getLastChar(String input) {
        int len = input.length();
        return input.substring(len-1, len);
    }

    private String runBashCommand(String command, int waitSecondsMax) {
        try {
            Process p = new ProcessBuilder("bash", "-c", command).start();
            p.waitFor(waitSecondsMax, TimeUnit.SECONDS);
            if (p.exitValue() == 0) {
                return SdkBytes.fromInputStream(p.getInputStream()).asUtf8String();
            } else {
                throw new RuntimeException("Bash command exited with non-zero status code");
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to run bash command `"+command+"`", e);
        }
    }

}
