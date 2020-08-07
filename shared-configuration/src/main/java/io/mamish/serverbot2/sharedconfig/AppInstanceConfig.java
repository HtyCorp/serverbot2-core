package io.mamish.serverbot2.sharedconfig;

public class AppInstanceConfig {

    public static final String PATH_ALL = "app-instance-share";

    // PRIVATE

    public static final String PATH_PRIVATE = PATH_ALL + "/private";

    // PUBLIC

    public static final String PATH_PUBLIC = PATH_ALL + "/public";

    public static final String QUEUE_NAME_PREFIX = "AppDaemonQueue";
    public static final String INSTANCE_NAME_PREFIX = "AppInstance";
    public static final String APP_NAME_INSTANCE_TAG_KEY = "Serverbot2AppName";

    // Will redesign this later to use instance-specific roles.
    // Would need to work this as a new service or add into network-security, maybe.
    public static final String COMMON_INSTANCE_PROFILE_NAME = "CommonAppInstanceProfile";

    public static final Parameter APP_DAEMON_JAR_S3_URL = new Parameter(PATH_PUBLIC, "app-daemon-jar-s3-url");

    public static final String APP_LOGS_GROUP_PREFIX = "serverbot2/app/serverlogs";

    public static final int APP_SHUTDOWN_TIMEOUT_SECONDS = 15;

    public static final int APP_IDLENESS_WARNING_TIME_SECONDS = 20 * 60;
    public static final int APP_IDLENESS_SHUTDOWN_TIME_SECONDS = 30 * 60;


}
