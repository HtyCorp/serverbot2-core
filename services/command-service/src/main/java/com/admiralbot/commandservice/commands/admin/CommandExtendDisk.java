package com.admiralbot.commandservice.commands.admin;

import com.admiralbot.commandservice.commands.AbstractCommandDto;
import com.admiralbot.framework.modelling.ApiArgumentInfo;
import com.admiralbot.framework.modelling.ApiRequestInfo;

@ApiRequestInfo(order = 13, name = "extenddisk", numRequiredFields = 2,
        description = "Expand a server's disk to the requested size in GB. Use with caution!")
public class CommandExtendDisk extends AbstractCommandDto {

    @ApiArgumentInfo(order = 0, description = "Name of game to extend main disk for")
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
