package io.mamish.serverbot2.networksecurity;

import io.mamish.serverbot2.framework.exception.server.NoSuchResourceException;
import io.mamish.serverbot2.framework.exception.server.RequestValidationException;
import io.mamish.serverbot2.framework.server.LambdaApiServer;
import io.mamish.serverbot2.networksecurity.crypto.Crypto;
import io.mamish.serverbot2.networksecurity.model.*;
import io.mamish.serverbot2.networksecurity.securitygroups.Ec2GroupManager;
import io.mamish.serverbot2.networksecurity.securitygroups.IGroupManager;
import io.mamish.serverbot2.networksecurity.securitygroups.MockGroupManager;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedconfig.NetSecConfig;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.model.KmsException;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class LambdaHandler extends LambdaApiServer<INetworkSecurity> {

    @Override
    protected Class<INetworkSecurity> getModelClass() {
        return INetworkSecurity.class;
    }

    @Override
    protected INetworkSecurity createHandlerInstance() {
        return new NetworkSecurityServiceHandler();
    }

}
