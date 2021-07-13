package com.admiralbot.madscientist.lambdaruntime;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class LambdaInvocation {

    public static final String INVOCATION_ID_HEADER = "Lambda-Runtime-Aws-Request-Id";
    public static final String DEADLINE_MS_HEADER = "Lambda-Runtime-Deadline-Ms";
    public static final String LAMBDA_ARN_HEADER = "Lambda-Runtime-Invoked-Function-Arn";
    public static final String XRAY_TRACE_ID_HEADER = "Lambda-Runtime-Trace-Id";

    APIGatewayProxyRequestEvent apiGatewayRequest;
    String id;
    long deadlineMs;
    String lambdaArn;
    String xrayTraceId;

}
