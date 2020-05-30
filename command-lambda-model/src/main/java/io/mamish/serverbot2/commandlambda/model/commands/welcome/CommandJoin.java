package io.mamish.serverbot2.commandlambda.model.commands.welcome;

import io.mamish.serverbot2.commandlambda.model.commands.AbstractCommandDto;
import io.mamish.serverbot2.framework.common.ApiArgumentInfo;
import io.mamish.serverbot2.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 0, name = "join", numRequiredFields = 1,
        description = "Join a server bot channel (normally hidden to reduce spam) so you can view and interact with it")
public class CommandJoin extends AbstractCommandDto {

    @ApiArgumentInfo(order = 0, description = "The channel to join (you probably want 'servers', but 'debug' is also joinable)")
    private String channel;

    public String getChannel() {
        return channel;
    }
}
