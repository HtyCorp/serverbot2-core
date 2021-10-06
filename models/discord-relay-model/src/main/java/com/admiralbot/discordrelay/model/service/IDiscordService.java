package com.admiralbot.discordrelay.model.service;

import com.admiralbot.framework.modelling.ApiAuthType;
import com.admiralbot.framework.modelling.ApiEndpointInfo;
import com.admiralbot.framework.modelling.ApiHttpMethod;

@ApiEndpointInfo(serviceName = "discordrelay", uriPath = "/", httpMethod = ApiHttpMethod.POST, authType = ApiAuthType.IAM)
public interface IDiscordService {

    NewMessageResponse newMessage(NewMessageRequest newMessageRequest);
    EditMessageResponse editMessage(EditMessageRequest editMessageRequest);
    ModifyRoleMembershipResponse modifyRoleMembership(ModifyRoleMembershipRequest removeFromRoleRequest);
    PutSlashCommandsResponse putSlashCommands(PutSlashCommandsRequest putSlashCommandsRequest);

}
