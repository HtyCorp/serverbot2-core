package io.mamish.serverbot2.sharedutil;

public class XrayUtils {

    private XrayUtils() {}

    // Configuration values are documented at:
    // https://docs.aws.amazon.com/xray/latest/devguide/aws-x-ray-auto-instrumentation-agent-for-java.html#XRayAutoInstrumentationAgent-Configuration

    public static void setServiceName(String serviceName) {
        setProperty("com.amazonaws.xray.strategy.tracingName", serviceName);
    }

    public static void setInstrumentationEnabled(boolean enabled) {
        setProperty("com.amazonaws.xray.tracingEnabled", Boolean.toString(enabled));
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

    private static void setProperty(String name, String value) {
        System.getProperties().setProperty(name, value);
    }

}
