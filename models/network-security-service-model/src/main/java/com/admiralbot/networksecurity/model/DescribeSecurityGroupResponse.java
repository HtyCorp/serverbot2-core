package com.admiralbot.networksecurity.model;

public class DescribeSecurityGroupResponse {

    private ManagedSecurityGroup securityGroup;

    public DescribeSecurityGroupResponse() { }

    public DescribeSecurityGroupResponse(ManagedSecurityGroup securityGroup) {
        this.securityGroup = securityGroup;
    }

    public ManagedSecurityGroup getSecurityGroup() {
        return securityGroup;
    }
}
