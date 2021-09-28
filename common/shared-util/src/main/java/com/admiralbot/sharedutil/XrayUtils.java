package com.admiralbot.sharedutil;

import java.util.Map;
import java.util.concurrent.Callable;

public class XrayUtils {

    public static final String TRACE_ID_HEADER_KEY = "X-Amzn-Trace-Id";
    public static final String TRACE_ID_LAMBDA_ENV_VAR = "_X_AMZN_TRACE_ID";

    private XrayUtils() {}

    // Configuration values are documented at:
    // https://docs.aws.amazon.com/xray/latest/devguide/aws-x-ray-auto-instrumentation-agent-for-java.html#XRayAutoInstrumentationAgent-Configuration

    public static void setServiceName(String serviceName) {
        setProperty("com.amazonaws.xray.strategy.tracingName", serviceName);
    }

    public static void setInstrumentationEnabled(boolean enabled) {
        // The ideal is to modify the equivalent env var, but modifying env vars in Java is not simple
        // Recent versions of AWS Xray Java SDK support this system property as an alternative
        setProperty("com.amazonaws.xray.tracingEnabled", xrayBoolString(enabled));
    }

    public static void setTraceId(String traceId) {
        System.setProperty("com.amazonaws.xray.traceHeader", traceId);
    }

    enum XrayMissingContextStrategy {
        RUNTIME_ERROR, LOG_ERROR, IGNORE_ERROR
    }

    public static void setMissingContextStrategy(XrayMissingContextStrategy strategy) {
        setProperty("com.amazonaws.xray.strategy.contextMissingStrategy", strategy.name());
    }

    public static void setIgnoreMissingContext() {
        setMissingContextStrategy(XrayMissingContextStrategy.IGNORE_ERROR);
    }

    /*
     * No-op Xray proxy calls originally used for manual Xray instrumentation.
     * These are proxied so Xray can be removed now as a dependency but potentially re-enabled later.
     */

    public static String getTraceHeader() {
        return System.getenv(TRACE_ID_LAMBDA_ENV_VAR);
    }

    public static void beginSegment(String name) { }

    public static void beginSegment(String name, String parentTraceId) { }

    public static void addSegmentException(Exception e) { }

    public static void endSegment() { }

    public static void beginSubsegment(String name) { }

    public static void addSubsegmentException(Exception e) { }

    public static void endSubsegment() { }

    public static <T> T subsegment(String name, Map<String,String> annotations, Callable<T> callable) {
        try {
            return callable.call();
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute subsegment routine", e);
        }
    }

    public static void subsegment(String name, Map<String,String> annotations, Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute subsegment routine", e);
        }
    }

    private static void setProperty(String name, String value) {
        System.getProperties().setProperty(name, value);
    }

    private static String xrayBoolString(boolean value) {
        return value ? "True": "False";
    }

}