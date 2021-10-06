package com.admiralbot.appdaemon.model;

import com.admiralbot.framework.modelling.ApiRequestInfo;

@ApiRequestInfo(order = 2, name = "StartSftpServer", numRequiredFields = 0,
        description = "Start SFTP server on this app instance. Returns the existing server details if already started.")
public class StartSftpServerRequest {
    // EMPTY
}
