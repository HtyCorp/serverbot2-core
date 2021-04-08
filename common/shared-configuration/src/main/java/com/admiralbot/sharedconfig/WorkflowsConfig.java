package com.admiralbot.sharedconfig;

public class WorkflowsConfig {

    public static final long NEW_INSTANCE_TIMEOUT_SECONDS = 360; // 6 minutes
    public static final long DAEMON_HEARTBEAT_TIMEOUT_SECONDS = 150; // 2.5 minutes

    // This is set very high because polling timeouts waiting for instance state == STOPPED keep happening.
    // We need to modify this at some point to be driven by CloudWatch Events rather than by polling.
    public static final int STEP_LAMBDA_TIMEOUT_SECONDS = 240;
}
