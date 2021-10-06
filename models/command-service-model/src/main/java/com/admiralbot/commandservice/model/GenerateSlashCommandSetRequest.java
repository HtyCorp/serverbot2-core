package com.admiralbot.commandservice.model;

import com.admiralbot.framework.modelling.ApiRequestInfo;

@ApiRequestInfo(order = 1, name = "GenerateSlashCommandSet", numRequiredFields = 0,
        description = "Generate the full set of available slash commands from CommandService command models")
public class GenerateSlashCommandSetRequest {

    // EMPTY

}
