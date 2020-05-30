package io.mamish.serverbot2.discordrelay.model.service;

import io.mamish.serverbot2.framework.common.ApiArgumentInfo;
import io.mamish.serverbot2.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 2, name = "ModifyRoleMembership", numRequiredFields = 3,
        description = "Add or remove a user from a channel-view role")
public class ModifyRoleMembershipRequest {

    @ApiArgumentInfo(order = 0, description = "Discord ID of user to add")
    private String userDiscordId;
    @ApiArgumentInfo(order = 1, description = "Channel user will be able or unable to view after changing membership")
    private MessageChannel roleChannel;
    @ApiArgumentInfo(order = 2, description = "How to change the user's role membership")
    private RoleModifyOperation roleModifyOperation;

    public ModifyRoleMembershipRequest() { }

    public ModifyRoleMembershipRequest(String userDiscordId, MessageChannel roleChannel, RoleModifyOperation roleModifyOperation) {
        this.userDiscordId = userDiscordId;
        this.roleChannel = roleChannel;
        this.roleModifyOperation = roleModifyOperation;
    }

    public String getUserDiscordId() {
        return userDiscordId;
    }

    public MessageChannel getRoleChannel() {
        return roleChannel;
    }

    public RoleModifyOperation getRoleModifyOperation() {
        return roleModifyOperation;
    }
}
