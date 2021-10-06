package com.admiralbot.echoservice.model;

import com.admiralbot.framework.modelling.ApiAuthType;
import com.admiralbot.framework.modelling.ApiEndpointInfo;
import com.admiralbot.framework.modelling.ApiHttpMethod;

@ApiEndpointInfo(serviceName = "echodebug", uriPath = "/", httpMethod = ApiHttpMethod.POST, authType = ApiAuthType.IAM)
public interface IEchoService {
    EchoResponse echo(EchoRequest request);
    HowMuchStorageResponse howMuchStorage(HowMuchStorageRequest request);
}
