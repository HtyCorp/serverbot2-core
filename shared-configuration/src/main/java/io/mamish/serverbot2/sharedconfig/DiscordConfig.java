package io.mamish.serverbot2.sharedconfig;

public class DiscordConfig {

    public static Secret API_TOKEN = new Secret("discord-relay/api-token");

    public static Parameter CHANNEL_ID_STANDARD = new Parameter("discord-relay/channel-id/standard");
    public static Parameter CHANNEL_ID_OFFICER = new Parameter("discord-relay/channel-id/officer");
    public static Parameter CHANNEL_ID_ADMIN = new Parameter("discord-relay/channel-id/admin");
    public static Parameter CHANNEL_ID_DEBUG = new Parameter("discord-relay/channel-id/debug");

    // I'm normally avoiding fixed resource names (i.e. using CDK/CFN names), but persistent data stores are an exception.
    // Deletion/replacement of these should be manual and very careful anyway.
    public static final String MESSAGE_TABLE_NAME = "DiscordRelayMessages";
    public static final String MESSAGE_TABLE_PKEY = "externalId";

}
