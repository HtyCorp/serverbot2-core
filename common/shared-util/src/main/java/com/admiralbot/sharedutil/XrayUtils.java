package com.admiralbot.sharedutil;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.Subsegment;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class XrayUtils {

    public static final String TRACE_HEADER_HTTP_HEADER_KEY = "X-Amzn-Trace-Id";

    private static final String SERVICE_NAME_PROPERTY = "com.amazonaws.xray.strategy.tracingName";
    private static final String TRACE_HEADER_ENV_VAR = "_X_AMZN_TRACE_ID";
    private static final String TRACE_HEADER_PROPERTY = "com.amazonaws.xray.traceHeader";
    private static final String TRACING_ENABLED_PROPERTY = "com.amazonaws.xray.tracingEnabled";
    private static final String MISSING_CONTEXT_STRATEGY_PROPERTY = "com.amazonaws.xray.strategy.contextMissingStrategy";

    private XrayUtils() {}

    // Configuration values are documented at:
    // https://docs.aws.amazon.com/xray/latest/devguide/aws-x-ray-auto-instrumentation-agent-for-java.html#XRayAutoInstrumentationAgent-Configuration

    public static void setServiceName(String serviceName) {
        setProperty(SERVICE_NAME_PROPERTY, serviceName);
    }

    public static void setInstrumentationEnabled(boolean enabled) {
        // The ideal is to modify the equivalent env var, but modifying env vars in Java is not simple
        // Recent versions of AWS Xray Java SDK support this system property as an alternative
        setProperty(TRACING_ENABLED_PROPERTY, xrayBoolString(enabled));
    }

    public static void setTraceId(String traceId) {
        System.setProperty(TRACE_HEADER_PROPERTY, traceId);
    }

    enum XrayMissingContextStrategy {
        RUNTIME_ERROR, LOG_ERROR, IGNORE_ERROR
    }

    public static void setMissingContextStrategy(XrayMissingContextStrategy strategy) {
        setProperty(MISSING_CONTEXT_STRATEGY_PROPERTY, strategy.name());
    }

    public static void setIgnoreMissingContext() {
        setMissingContextStrategy(XrayMissingContextStrategy.IGNORE_ERROR);
    }

    /*
     * No-op Xray proxy calls originally used for manual Xray instrumentation.
     * These are proxied so Xray can be removed now as a dependency but potentially re-enabled later.
     */

    public static String getTraceHeader() {
        return Optional.ofNullable(System.getenv(TRACE_HEADER_ENV_VAR))
                .orElse(System.getProperty(TRACE_HEADER_PROPERTY));
    }

    public static Segment beginSegment(String name) {
        return AWSXRay.beginSegment(name);
    }

    public static void addSegmentException(Exception e) {
        AWSXRay.getCurrentSegment().addException(e);
    }

    public static void endSegment() {
        AWSXRay.endSegment();
    }

    public static Subsegment beginSubsegment(String name) {
        return AWSXRay.beginSubsegment(name);
    }

    public static void addSubsegmentException(Exception e) {
        AWSXRay.getCurrentSubsegment().addException(e);
    }

    public static void endSubsegment() {
        AWSXRay.endSubsegment();
    }

    public static <T> T subsegment(String name, Map<String,Object> annotations, Supplier<T> action) {
        var subsegment = AWSXRay.beginSubsegment(name);
        if (annotations != null) {
            subsegment.setAnnotations(annotations);
        }
        try {
            return action.get();
        } catch (Exception e) {
            subsegment.addException(e);
            throw e;
        } finally {
            AWSXRay.endSubsegment();
        }
    }

    public static void subsegment(String name, Map<String,Object> annotations, Runnable action) {
        var subsegment = AWSXRay.beginSubsegment(name);
        if (annotations != null) {
            subsegment.setAnnotations(annotations);
        }
        try {
            action.run();
        } catch (Exception e) {
            subsegment.addException(e);
            throw e;
        } finally {
            AWSXRay.endSubsegment();
        }
    }

    private static void setProperty(String name, String value) {
        System.getProperties().setProperty(name, value);
    }

    private static String xrayBoolString(boolean value) {
        return value ? "True": "False";
    }

}