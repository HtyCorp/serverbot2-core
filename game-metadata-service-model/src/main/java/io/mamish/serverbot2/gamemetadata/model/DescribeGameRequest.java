package io.mamish.serverbot2.gamemetadata.model;

import io.mamish.serverbot2.framework.common.ApiArgumentInfo;
import io.mamish.serverbot2.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 1, name = "DescribeGame", numRequiredFields = 1, description = "Get metadata for a specific game")
public class DescribeGameRequest {

    @ApiArgumentInfo(order = 0, name = "gameName", description = "Name of game to fetch metadata of")
    private String gameName;

    public DescribeGameRequest(String gameName) {
        this.gameName = gameName;
    }

    public String getGameName() {
        return gameName;
    }
}
