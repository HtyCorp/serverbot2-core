package com.admiralbot.urlshortener.model;

import com.admiralbot.framework.modelling.ApiAuthType;
import com.admiralbot.framework.modelling.ApiEndpointInfo;
import com.admiralbot.framework.modelling.ApiHttpMethod;

@ApiEndpointInfo(serviceName = "urlshortener", uriPath = "/", httpMethod = ApiHttpMethod.POST, authType = ApiAuthType.IAM)
public interface IUrlShortener {

    CreateShortUrlResponse createShortUrl(CreateShortUrlRequest request);
    GetFullUrlResponse getFullUrl(GetFullUrlRequest request);

}
