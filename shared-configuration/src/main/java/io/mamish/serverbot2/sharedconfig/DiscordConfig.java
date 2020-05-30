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
    public static final Parameter CHANNEL_ID_SERVERS = new Parameter(PATH_PUBLIC, "channel-id/servers");
    public static final Parameter CHANNEL_ID_ADMIN = new Parameter(PATH_PUBLIC, "channel-id/admin");
    public static final Parameter CHANNEL_ID_DEBUG = new Parameter(PATH_PUBLIC, "channel-id/debug");

    // Note only these two are supplied since the other channels don't support/require dynamic join/leave.
    public static final Parameter CHANNEL_ROLE_SERVERS = new Parameter(PATH_PUBLIC, "role-id/servers");
    public static final Parameter CHANNEL_ROLE_DEBUG = new Parameter(PATH_PUBLIC, "role-id/debug");

    // I'm normally avoiding fixed resource names (i.e. using CDK/CFN names), but persistent data stores are an exception.
    // Deletion/replacement of these should be manual and very careful anyway.
    public static final String MESSAGE_TABLE_NAME = "DiscordRelayMessages";
    public static final String MESSAGE_TABLE_PKEY = "externalId";

    public static final String SQS_QUEUE_NAME = "DiscordRelayRequestsQueue";
//    public static final ConfigValue SQS_QUEUE_URL_RESOLVED = new ConfigValue(SQS_QUEUE_NAME,
//            n -> SqsClient.create().getQueueUrl(r -> r.queueName(n)).queueUrl());

}
