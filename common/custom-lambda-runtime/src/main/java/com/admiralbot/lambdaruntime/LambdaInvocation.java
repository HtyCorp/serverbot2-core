package com.admiralbot.lambdaruntime;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;

public class LambdaInvocation<BodyType> {

    private final BodyType requestBody;
    private final String id;
    private final long deadlineMs;
    private final String lambdaArn;
    private final String xrayTraceId;

    // No default constructor necessary: not used for reflection

    public LambdaInvocation(BodyType requestBody, String id, long deadlineMs, String lambdaArn,
                            String xrayTraceId) {
        this.requestBody = requestBody;
        this.id = id;
        this.deadlineMs = deadlineMs;
        this.lambdaArn = lambdaArn;
        this.xrayTraceId = xrayTraceId;
    }

    public BodyType getRequestBody() {
        return requestBody;
    }

    public String getId() {
        return id;
    }

    public long getDeadlineMs() {
        return deadlineMs;
    }

    public String getLambdaArn() {
        return lambdaArn;
    }

    public String getXrayTraceId() {
        return xrayTraceId;
    }
}