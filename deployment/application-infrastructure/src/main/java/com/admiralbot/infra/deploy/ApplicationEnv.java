package com.admiralbot.infra.deploy;

public class ApplicationEnv {

    private boolean enabled;
    private boolean requiresApproval;

    private String name;
    private String accountId;
    private String region;

    private String discordApiTokenSourceSecretArn;
    private String webPushKeyPairSourceSecretArn;

    private String artifactBucketName;

    private String systemRootDomainName;
    private String systemRootDomainZoneId;
    private String appRootDomainName;
    private String appRootDomainZoneId;

    private String discordRelayChannelIdWelcome;
    private String discordRelayChannelIdMain;
    private String discordRelayChannelIdAdmin;

    private String discordRelayRoleIdMain;

    private int prefixListCapacity;

    public boolean isEnabled() {
        return enabled;
    }

    public boolean requiresApproval() {
        return requiresApproval;
    }

    public String getName() {
        return name;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getRegion() {
        return region;
    }

    public String getDiscordApiTokenSourceSecretArn() {
        return discordApiTokenSourceSecretArn;
    }

    public String getWebPushKeyPairSourceSecretArn() {
        return webPushKeyPairSourceSecretArn;
    }

    public String getArtifactBucketName() {
        return artifactBucketName;
    }

    public String getSystemRootDomainName() {
        return systemRootDomainName;
    }

    public String getSystemRootDomainZoneId() {
        return systemRootDomainZoneId;
    }

    public String getAppRootDomainName() {
        return appRootDomainName;
    }

    public String getAppRootDomainZoneId() {
        return appRootDomainZoneId;
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

    public String getDiscordRelayRoleIdMain() {
        return discordRelayRoleIdMain;
    }

    public int getPrefixListCapacity() {
        return prefixListCapacity;
    }
}
