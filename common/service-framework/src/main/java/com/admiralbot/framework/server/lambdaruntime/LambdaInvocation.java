package com.admiralbot.framework.server.lambdaruntime;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;

public class LambdaInvocation {

    public static final String INVOCATION_ID_HEADER = "Lambda-Runtime-Aws-Request-Id";
    public static final String DEADLINE_MS_HEADER = "Lambda-Runtime-Deadline-Ms";
    public static final String LAMBDA_ARN_HEADER = "Lambda-Runtime-Invoked-Function-Arn";
    public static final String XRAY_TRACE_ID_HEADER = "Lambda-Runtime-Trace-Id";

    private APIGatewayProxyRequestEvent apiGatewayRequest;
    private String id;
    private long deadlineMs;
    private String lambdaArn;
    private String xrayTraceId;

    // No default constructor necessary: not used for for reflection

    public LambdaInvocation(APIGatewayProxyRequestEvent apiGatewayRequest, String id, long deadlineMs, String lambdaArn,
                            String xrayTraceId) {
        this.apiGatewayRequest = apiGatewayRequest;
        this.id = id;
        this.deadlineMs = deadlineMs;
        this.lambdaArn = lambdaArn;
        this.xrayTraceId = xrayTraceId;
    }

    public APIGatewayProxyRequestEvent getApiGatewayRequest() {
        return apiGatewayRequest;
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