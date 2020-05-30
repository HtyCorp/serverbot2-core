package io.mamish.serverbot2.discordrelay.model.service;

public interface IDiscordService {

    NewMessageResponse newMessage(NewMessageRequest newMessageRequest);
    EditMessageResponse editMessage(EditMessageRequest editMessageRequest);
    ModifyRoleMembershipResponse modifyRoleMembership(ModifyRoleMembershipRequest removeFromRoleRequest);

}
