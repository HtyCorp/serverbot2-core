package io.mamish.serverbot2.networksecurity.model;

import io.mamish.serverbot2.framework.common.ApiAuthType;
import io.mamish.serverbot2.framework.common.ApiEndpointInfo;
import io.mamish.serverbot2.framework.common.ApiHttpMethod;

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
