package io.mamish.serverbot2.networksecurity.model;

public class GenerateIpAuthUrlResponse {

    String ipAuthUrl;

    public GenerateIpAuthUrlResponse() { }

    public GenerateIpAuthUrlResponse(String ipAuthUrl) {
        this.ipAuthUrl = ipAuthUrl;
    }

    public String getIpAuthUrl() {
        return ipAuthUrl;
    }
}
