package com.admiralbot.sharedutil;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorder;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
import com.amazonaws.xray.contexts.SegmentContextResolverChain;
import com.amazonaws.xray.contexts.ThreadLocalSegmentContextResolver;
import com.amazonaws.xray.entities.Entity;
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.Subsegment;
import com.amazonaws.xray.entities.TraceHeader;
import com.amazonaws.xray.strategy.IgnoreErrorContextMissingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.function.Supplier;

import static com.amazonaws.xray.entities.TraceHeader.SampleDecision.NOT_SAMPLED;
import static com.amazonaws.xray.entities.TraceHeader.SampleDecision.SAMPLED;

public class XrayUtils {

    public static final String TRACE_HEADER_HTTP_HEADER_KEY = "X-Amzn-Trace-Id";

    private static final String SERVICE_NAME_PROPERTY = "com.amazonaws.xray.strategy.tracingName";
    private static final String TRACE_HEADER_ENV_VAR = "_X_AMZN_TRACE_ID";
    private static final String TRACE_HEADER_PROPERTY = "com.amazonaws.xray.traceHeader";
    private static final String TRACING_ENABLED_PROPERTY = "com.amazonaws.xray.tracingEnabled";
    private static final String MISSING_CONTEXT_STRATEGY_PROPERTY = "com.amazonaws.xray.strategy.contextMissingStrategy";

    private static final Logger log = LoggerFactory.getLogger(XrayUtils.class);

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

    enum XrayMissingContextStrategy {
        RUNTIME_ERROR, LOG_ERROR, IGNORE_ERROR
    }

    public static void setMissingContextStrategy(XrayMissingContextStrategy strategy) {
        setProperty(MISSING_CONTEXT_STRATEGY_PROPERTY, strategy.name());
    }

    public static void setup() {
        setMissingContextStrategy(XrayMissingContextStrategy.IGNORE_ERROR);

        // Running this SDK in a Lambda without Xray enabled (not possible right not; no HTTP API integration)
        // doesn't work well since the SDK assumes a Segment already exists. So we omit the Lambda resolver.
        SegmentContextResolverChain contextChainNolambda = new SegmentContextResolverChain();
        contextChainNolambda.addResolver(new ThreadLocalSegmentContextResolver());

        AWSXRayRecorder modifiedRecorder = AWSXRayRecorderBuilder.standard()
                .withContextMissingStrategy(new IgnoreErrorContextMissingStrategy())
                .withSegmentContextResolverChain(contextChainNolambda)
                .build();
        AWSXRay.setGlobalRecorder(modifiedRecorder);
    }

    /*
     * No-op Xray proxy calls originally used for manual Xray instrumentation.
     * These are proxied so Xray can be removed now as a dependency but potentially re-enabled later.
     */

    // Ref: https://github.com/aws/aws-xray-sdk-java/blob/d9d17ec980dce1c7e40a4b2e67cd5f76c5a36ea9/aws-xray-recorder-sdk-apache-http/src/main/java/com/amazonaws/xray/proxies/apache/http/TracedHttpClient.java#L109
    public static String getTraceHeaderString() {
        Entity entity = AWSXRay.getTraceEntity();
        if (!(entity instanceof Subsegment)) {
            log.info("Trace header: skipping since entity is not a subsegment: <{}>", entity);
            return null;
        } else {
            Segment parentSegment = entity.getParentSegment();
            TraceHeader header = new TraceHeader(parentSegment.getTraceId(),
                    parentSegment.isSampled() ? entity.getId() : null,
                    parentSegment.isSampled() ? SAMPLED : NOT_SAMPLED);
            String headerString = header.toString();
            log.info("Generated trace header <{}>", headerString);
            return headerString;
        }
    }

    public static Entity getEntity() {
        return AWSXRay.getTraceEntity();
    }

    // Should only be used for copying entities between threads
    public static void setEntity(Entity entity) {
        if (entity != null) {
            AWSXRay.setTraceEntity(entity);
        } else {
            AWSXRay.clearTraceEntity();
        }
    }

    public static boolean isInSegment() {
        return AWSXRay.getTraceEntity() != null;
    }

    public static Segment beginSegment(String name, String traceHeaderString) {
        TraceHeader header = TraceHeader.fromString(traceHeaderString);
        if (header.getRootTraceId() == null || header.getParentId() == null) {
            log.warn("Trace header string does not contain a valid root ID or parent: <" +
                   traceHeaderString + ">");
            return AWSXRay.beginSegment(name);
        }
        return AWSXRay.beginSegment(name, header.getRootTraceId(), header.getParentId());
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