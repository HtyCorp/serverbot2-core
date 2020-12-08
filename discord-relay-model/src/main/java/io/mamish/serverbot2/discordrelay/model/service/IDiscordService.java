package io.mamish.serverbot2.discordrelay.model.service;

import io.mamish.serverbot2.framework.common.ApiAuthType;
import io.mamish.serverbot2.framework.common.ApiEndpointInfo;
import io.mamish.serverbot2.framework.common.ApiHttpMethod;

@ApiEndpointInfo(serviceName = "discordservice", uriPath = "/", httpMethod = ApiHttpMethod.POST, authType = ApiAuthType.IAM)
public interface IDiscordService {

    NewMessageResponse newMessage(NewMessageRequest newMessageRequest);
    EditMessageResponse editMessage(EditMessageRequest editMessageRequest);
    ModifyRoleMembershipResponse modifyRoleMembership(ModifyRoleMembershipRequest removeFromRoleRequest);

}
