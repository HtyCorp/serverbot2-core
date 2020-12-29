package com.admiralbot.urlshortener.model;

import com.admiralbot.framework.common.ApiAuthType;
import com.admiralbot.framework.common.ApiEndpointInfo;
import com.admiralbot.framework.common.ApiHttpMethod;

@ApiEndpointInfo(serviceName = "urlshortener", uriPath = "/", httpMethod = ApiHttpMethod.POST, authType = ApiAuthType.IAM)
public interface IUrlShortener {

    CreateShortUrlResponse createShortUrl(CreateShortUrlRequest request);
    GetFullUrlResponse getFullUrl(GetFullUrlRequest request);

}
