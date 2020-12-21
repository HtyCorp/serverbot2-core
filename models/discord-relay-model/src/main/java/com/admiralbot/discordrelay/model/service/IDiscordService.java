package com.admiralbot.discordrelay.model.service;

import com.admiralbot.framework.common.ApiAuthType;
import com.admiralbot.framework.common.ApiEndpointInfo;
import com.admiralbot.framework.common.ApiHttpMethod;

@ApiEndpointInfo(serviceName = "discordrelay", uriPath = "/", httpMethod = ApiHttpMethod.POST, authType = ApiAuthType.IAM)
public interface IDiscordService {

    NewMessageResponse newMessage(NewMessageRequest newMessageRequest);
    EditMessageResponse editMessage(EditMessageRequest editMessageRequest);
    ModifyRoleMembershipResponse modifyRoleMembership(ModifyRoleMembershipRequest removeFromRoleRequest);

}
