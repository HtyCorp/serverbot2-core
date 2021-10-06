package com.admiralbot.gamemetadata.model;

import com.admiralbot.framework.modelling.ApiArgumentInfo;
import com.admiralbot.framework.modelling.ApiRequestInfo;

@ApiRequestInfo(order = 1, name = "DescribeGame", numRequiredFields = 1, description = "Get metadata for a specific game")
public class DescribeGameRequest {

    @ApiArgumentInfo(order = 0, description = "Name of game to fetch metadata of")
    private String gameName;

    public DescribeGameRequest() { }

    public DescribeGameRequest(String gameName) {
        this.gameName = gameName;
    }

    public String getGameName() {
        return gameName;
    }
}
