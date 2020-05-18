package io.mamish.serverbot2.appinfra;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.lambda.Function;

public class NetSecStack extends Stack {

    public NetSecStack(Construct parent, String id, StackProps props) {
        super(parent, id, props);

        SecurityGroup referenceGroup = SecurityGroup.Builder.create(this, "ReferenceGroup")
                .

                ;

        Role functionRole = Util.standardLambdaRole(this, "NetSecServiceLambda", List.of(

        )).build();

        Function serviceFunction = Util.standardJavaFunction(this, "NetSecService", "network-security-service",
                "io.mamish.serverbot2.networksecurity.LambdaHandler")
                .role(functionRole).build();

        serviceFunction.add
    }

}
