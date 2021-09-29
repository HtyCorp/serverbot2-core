package com.admiralbot.echoservice.model;

import com.admiralbot.framework.common.ApiAuthType;
import com.admiralbot.framework.common.ApiEndpointInfo;
import com.admiralbot.framework.common.ApiHttpMethod;

@ApiEndpointInfo(serviceName = "echodebug", uriPath = "/", httpMethod = ApiHttpMethod.POST, authType = ApiAuthType.IAM)
public interface IEchoService {
    EchoResponse echo(EchoRequest request);
    HowMuchStorageResponse howMuchStorage(HowMuchStorageRequest request);
}
