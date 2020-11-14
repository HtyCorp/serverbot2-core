package io.mamish.serverbot2.appdaemon.model;

import io.mamish.serverbot2.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 1, name = "StartSftpServer", numRequiredFields = 0,
        description = "Start SFTP server on this app instance. Returns the existing server details if already started.")
public class StartSftpServerRequest {
    // EMPTY
}
