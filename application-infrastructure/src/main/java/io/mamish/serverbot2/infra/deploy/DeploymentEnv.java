package io.mamish.serverbot2.infra.deploy;

public class DeploymentEnv {

    private String name;
    private String accountId;
    private String region;

    private String discordApiToken;

    private String persistentArtifactBucket;

    private String domainName;
    private String route53ZoneId;

    private String discordRelayChannelIdWelcome;
    private String discordRelayChannelIdMain;
    private String discordRelayChannelIdAdmin;
    private String discordRelayChannelIdDebug;

    private String discordRelayRoleIdMain;
    private String discordRelayRoleIdDebug;

    public String getName() {
        return name;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getRegion() {
        return region;
    }

    public String getDiscordApiToken() {
        return discordApiToken;
    }

    public String getPersistentArtifactBucket() {
        return persistentArtifactBucket;
    }

    public String getDomainName() {
        return domainName;
    }

    public String getRoute53ZoneId() {
        return route53ZoneId;
    }

    public String getDiscordRelayChannelIdWelcome() {
        return discordRelayChannelIdWelcome;
    }

    public String getDiscordRelayChannelIdMain() {
        return discordRelayChannelIdMain;
    }

    public String getDiscordRelayChannelIdAdmin() {
        return discordRelayChannelIdAdmin;
    }

    public String getDiscordRelayChannelIdDebug() {
        return discordRelayChannelIdDebug;
    }

    public String getDiscordRelayRoleIdMain() {
        return discordRelayRoleIdMain;
    }

    public String getDiscordRelayRoleIdDebug() {
        return discordRelayRoleIdDebug;
    }
}
