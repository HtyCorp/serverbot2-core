package com.admiralbot.madscientist.model;

import com.admiralbot.framework.common.ApiAuthType;
import com.admiralbot.framework.common.ApiEndpointInfo;
import com.admiralbot.framework.common.ApiHttpMethod;

@ApiEndpointInfo(serviceName = "madscientist", uriPath = "/", httpMethod = ApiHttpMethod.POST, authType = ApiAuthType.IAM)
public interface IMadScientist {
    CountBucketsResponse countBuckets(CountBucketsRequest request);
}
