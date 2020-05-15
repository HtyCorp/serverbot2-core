package io.mamish.serverbot2.networksecurity.model;

public interface INetworkSecurity {

    CreateSecurityGroupResponse createSecurityGroup(CreateSecurityGroupResponse request);
    GenerateIpAuthUrlResponse generateIpAuthUrl(GenerateIpAuthUrlRequest request);
    AuthorizeIpResponse authorizeIp(AuthorizeIpRequest request);

}
