package com.admiralbot.networksecurity.model;

import com.admiralbot.framework.modelling.ApiAuthType;
import com.admiralbot.framework.modelling.ApiEndpointInfo;
import com.admiralbot.framework.modelling.ApiHttpMethod;

@ApiEndpointInfo(serviceName = "networksecurity", uriPath = "/", httpMethod = ApiHttpMethod.POST, authType = ApiAuthType.IAM)
public interface INetworkSecurity {

    CreateSecurityGroupResponse createSecurityGroup(CreateSecurityGroupRequest request);
    DescribeSecurityGroupResponse describeSecurityGroup(DescribeSecurityGroupRequest request);
    ModifyPortsResponse modifyPorts(ModifyPortsRequest request);
    GenerateIpAuthUrlResponse generateIpAuthUrl(GenerateIpAuthUrlRequest request);
    AuthorizeIpResponse authorizeIp(AuthorizeIpRequest request);
    DeleteSecurityGroupResponse deleteSecurityGroup(DeleteSecurityGroupRequest request);
    GetNetworkUsageResponse getNetworkUsage(GetNetworkUsageRequest getNetworkUsageRequest);
    RevokeExpiredIpsResponse revokeExpiredIps(RevokeExpiredIpsRequest revokeExpiredIpsRequest);
    GetAuthorizationByIpResponse getAuthorizationByIp(GetAuthorizationByIpRequest getAuthorizationByIpRequest);

}
