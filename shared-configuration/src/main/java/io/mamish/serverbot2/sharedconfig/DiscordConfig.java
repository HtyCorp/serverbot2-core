package io.mamish.serverbot2.sharedconfig;

public class DiscordConfig {

    public static Secret API_TOKEN = new Secret("discord-relay/api-token");

    public static Parameter CHANNEL_ID_STANDARD = new Parameter("discord-relay/channel-id/standard");
    public static Parameter CHANNEL_ID_OFFICER = new Parameter("discord-relay/channel-id/officer");
    public static Parameter CHANNEL_ID_ADMIN = new Parameter("discord-relay/channel-id/admin");
    public static Parameter CHANNEL_ID_DEBUG = new Parameter("discord-relay/channel-id/debug");

    public static String MESSAGE_TABLE_NAME = "DiscordRelayMessages";

}
