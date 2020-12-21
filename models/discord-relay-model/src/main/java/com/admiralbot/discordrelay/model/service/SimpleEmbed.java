package com.admiralbot.discordrelay.model.service;

public class SimpleEmbed {

    private String url;
    private String title;
    private String description;

    public SimpleEmbed() {}

    public SimpleEmbed(String url, String title, String description) {
        this.url = url;
        this.title = title;
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}
