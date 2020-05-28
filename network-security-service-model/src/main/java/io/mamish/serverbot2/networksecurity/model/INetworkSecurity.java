package io.mamish.serverbot2.networksecurity.model;

public interface INetworkSecurity {

    CreateSecurityGroupResponse createSecurityGroup(CreateSecurityGroupRequest request);
    DescribeSecurityGroupResponse describeSecurityGroup(DescribeSecurityGroupRequest request);
    ModifyPortsResponse modifyPorts(ModifyPortsRequest request);
    GenerateIpAuthUrlResponse generateIpAuthUrl(GenerateIpAuthUrlRequest request);
    AuthorizeIpResponse authorizeIp(AuthorizeIpRequest request);
    DeleteSecurityGroupResponse deleteSecurityGroup(DeleteSecurityGroupRequest request);

}
