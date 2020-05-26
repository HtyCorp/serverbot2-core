package io.mamish.serverbot2.appdaemon;

public class AppDaemon {

    public static void main(String[] args) {
        new AppDaemon();
    }

    private final GameMetadataFetcher gameMetadataFetcher = new GameMetadataFetcher();
    private final AppDaemonMessageHandler appDaemonMessageHandler;

    public AppDaemon() {
        appDaemonMessageHandler = new AppDaemonMessageHandler(gameMetadataFetcher);
    }

}
