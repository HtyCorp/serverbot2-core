package io.mamish.serverbot2.commandlambda.model.commands.welcome;

import io.mamish.serverbot2.commandlambda.model.commands.AbstractCommandDto;
import io.mamish.serverbot2.framework.common.ApiArgumentInfo;
import io.mamish.serverbot2.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 1, name = "leave", numRequiredFields = 1,
        description = "Leave a server bot channel - this hides it from your view to reduce message spam")
public class CommandLeave extends AbstractCommandDto {

    @ApiArgumentInfo(order = 0, description = "The channel to leave (either 'servers' or 'debug')")
    private String channel;

    public String getChannel() {
        return channel;
    }
}
