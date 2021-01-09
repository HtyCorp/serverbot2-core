package com.admiralbot.urlshortener.model;

import com.admiralbot.framework.common.ApiAuthType;
import com.admiralbot.framework.common.ApiEndpointInfo;
import com.admiralbot.framework.common.ApiHttpMethod;

@ApiEndpointInfo(serviceName = "urlshortener", uriPath = "/", httpMethod = ApiHttpMethod.POST, authType = ApiAuthType.IAM)
public interface IUrlShortener {

    DeliverUrlResponse deliverUrl(DeliverUrlRequest request);
    GetFullUrlResponse getFullUrl(GetFullUrlRequest request);

}
