package io.mamish.serverbot2.sharedconfig;

/**
 * Configuration values used by Discord relay and shared to other packages.
 */
public class DiscordConfig {

    public static final String PATH_ALL = "discord-relay";

    // PRIVATE

    public static final String PATH_PRIVATE = PATH_ALL+"/private";

    public static Secret API_TOKEN = new Secret(PATH_PRIVATE, "api-token");

    // PUBLIC

    public static final String PATH_PUBLIC = PATH_ALL+"/public";

    public static final Parameter CHANNEL_ID_WELCOME = new Parameter(PATH_PUBLIC, "channel-id/welcome");
    public static final Parameter CHANNEL_ID_MAIN = new Parameter(PATH_PUBLIC, "channel-id/main");
    public static final Parameter CHANNEL_ID_ADMIN = new Parameter(PATH_PUBLIC, "channel-id/admin");

    // Only the main channel support join/leave via commands.
    public static final Parameter CHANNEL_ROLE_MAIN = new Parameter(PATH_PUBLIC, "role-id/main");

    // I'm normally avoid fixed resource names (i.e. using CDK/CFN names), but persistent data stores are an exception.
    // Deletion/replacement of these should be manual and very careful anyway.
    public static final String MESSAGE_TABLE_NAME = "DiscordRelayMessages";
    public static final String MESSAGE_TABLE_PKEY = "externalMessageId";

}
