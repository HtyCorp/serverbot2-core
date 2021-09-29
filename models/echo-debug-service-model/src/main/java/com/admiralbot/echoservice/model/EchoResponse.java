package com.admiralbot.echoservice.model;

public class EchoResponse {

    private String echo;

    public EchoResponse() {}

    public EchoResponse(String echo) {
        this.echo = echo;
    }

    public String getEcho() {
        return echo;
    }
}
