package io.mamish.serverbot2.appdaemon.model;

public class StartSftpServerResponse {

    private SftpSession sftpSession;

    public StartSftpServerResponse() { }

    public StartSftpServerResponse(SftpSession sftpSession) {
        this.sftpSession = sftpSession;
    }

    public SftpSession getSftpSession() {
        return sftpSession;
    }

}
