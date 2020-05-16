package io.mamish.serverbot2.networksecurity;

import io.mamish.serverbot2.framework.exception.server.RequestHandlingException;
import io.mamish.serverbot2.framework.exception.server.RequestHandlingRuntimeException;
import io.mamish.serverbot2.framework.server.LambdaApiServer;
import io.mamish.serverbot2.networksecurity.model.*;
import io.mamish.serverbot2.sharedconfig.NetSecConfig;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.ec2.Ec2Client;

import java.util.List;

public class LambdaHandler extends LambdaApiServer<INetworkSecurity> implements INetworkSecurity {

    private final Ec2Client ec2Client = Ec2Client.create();

    @Override
    protected Class<INetworkSecurity> getModelClass() {
        return INetworkSecurity.class;
    }

    @Override
    protected INetworkSecurity getHandlerInstance() {
        return this;
    }

    @Override
    public CreateSecurityGroupResponse createSecurityGroup(CreateSecurityGroupRequest request) {
        String name = request.getGameName();
        try {
            String id = ec2Client.createSecurityGroup(r -> r.groupName(makeSgName(name))).groupId();
            SecurityGroup blank = new SecurityGroup(name, id, List.of(), List.of());
            return new CreateSecurityGroupResponse(blank);
        } catch (AwsServiceException e) {
            if (e.awsErrorDetails().errorCode().equals("InvalidGroup.Duplicate")) {
                throw new RequestHandlingException("Group for name '" + name + "' already exists.");
            }
            e.printStackTrace();
            throw new RequestHandlingRuntimeException("Could not creatte new security group", e);
        }
    }

    // TODO

    @Override
    public DescribeSecurityGroupResponse describeSecurityGroup(DescribeSecurityGroupRequest request) {
        return null;
    }

    @Override
    public ModifyPortsResponse modifyPorts(ModifyPortsRequest request) {
        return null;
    }

    @Override
    public GenerateIpAuthUrlResponse generateIpAuthUrl(GenerateIpAuthUrlRequest request) {
        return null;
    }

    @Override
    public AuthorizeIpResponse authorizeIp(AuthorizeIpRequest request) {
        return null;
    }

    private static String makeSgName(String gameName) {
        return NetSecConfig.SG_NAME_PREFIX + "-" + gameName;
    }

}
