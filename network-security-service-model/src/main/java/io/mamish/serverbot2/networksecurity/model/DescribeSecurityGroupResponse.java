package io.mamish.serverbot2.networksecurity.model;

public class DescribeSecurityGroupResponse {

    private ApplicationSecurityGroup securityGroup;

    public DescribeSecurityGroupResponse() { }

    public DescribeSecurityGroupResponse(ApplicationSecurityGroup securityGroup) {
        this.securityGroup = securityGroup;
    }

    public ApplicationSecurityGroup getSecurityGroup() {
        return securityGroup;
    }
}
