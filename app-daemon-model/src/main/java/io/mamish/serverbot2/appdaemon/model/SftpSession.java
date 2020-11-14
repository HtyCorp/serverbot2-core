package io.mamish.serverbot2.appdaemon.model;

public class SftpSession {

    private String username;
    private String password;
    private String sshFingerprint;

    public SftpSession() { }

    public SftpSession(String username, String password, String sshFingerprint) {
        this.username = username;
        this.password = password;
        this.sshFingerprint = sshFingerprint;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getSshFingerprint() {
        return sshFingerprint;
    }
}
