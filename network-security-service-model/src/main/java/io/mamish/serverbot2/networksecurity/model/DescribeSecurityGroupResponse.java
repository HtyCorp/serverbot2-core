package io.mamish.serverbot2.networksecurity.model;

public class DescribeSecurityGroupResponse {

    private SecurityGroup securityGroup;

    public DescribeSecurityGroupResponse() { }

    public DescribeSecurityGroupResponse(SecurityGroup securityGroup) {
        this.securityGroup = securityGroup;
    }

    public SecurityGroup getSecurityGroup() {
        return securityGroup;
    }
}
