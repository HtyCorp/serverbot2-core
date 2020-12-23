package com.admiralbot.commandservice.commands.admin;

import com.admiralbot.commandservice.commands.AbstractCommandDto;
import com.admiralbot.framework.common.ApiArgumentInfo;
import com.admiralbot.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 13, name = "extenddisk", numRequiredFields = 2,
        description = "Expand a server's main disk to the requested size in GB")
public class CommandExtendDisk extends AbstractCommandDto {

    @ApiArgumentInfo(order = 0, description = "Name of game to extend main disk of")
    private String gameName;

    @ApiArgumentInfo(order = 1, description = "Size in GB to expand disk to")
    private int sizeGB;

    public CommandExtendDisk() {}

    public String getGameName() {
        return gameName;
    }

    public int getSizeGB() {
        return sizeGB;
    }

}
