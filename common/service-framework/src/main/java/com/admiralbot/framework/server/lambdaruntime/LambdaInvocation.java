package com.admiralbot.framework.server.lambdaruntime;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;

public class LambdaInvocation {

    private final APIGatewayV2HTTPEvent apiGatewayEvent;
    private final String id;
    private final long deadlineMs;
    private final String lambdaArn;
    private final String xrayTraceId;

    // No default constructor necessary: not used for for reflection

    public LambdaInvocation(APIGatewayV2HTTPEvent apiGatewayEvent, String id, long deadlineMs, String lambdaArn,
                            String xrayTraceId) {
        this.apiGatewayEvent = apiGatewayEvent;
        this.id = id;
        this.deadlineMs = deadlineMs;
        this.lambdaArn = lambdaArn;
        this.xrayTraceId = xrayTraceId;
    }

    public APIGatewayV2HTTPEvent getApiGatewayEvent() {
        return apiGatewayEvent;
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