package com.admiralbot.urlshortener.model;

import com.admiralbot.discordrelay.model.service.SimpleEmbed;
import com.admiralbot.framework.common.ApiArgumentInfo;
import com.admiralbot.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 0, name = "CreateShortUrl", numRequiredFields = 6,
        description = "Deliver a URL to the requested user to navigate to, based on their set delivery preferences")
public class DeliverUrlRequest {

    @ApiArgumentInfo(order = 0, description = "User to deliver URL to")
    private String discordUserId;

    @ApiArgumentInfo(order = 1, description = "Full URL to direct user to")
    private String url;

    @ApiArgumentInfo(order = 2, description = "Time in seconds the URL will be valid for")
    private long ttlSeconds;

    @ApiArgumentInfo(order =  3, description = "Preferred delivery method; user preferences might force a lesser method")
    private DeliveryType preferredDeliveryType;

    @ApiArgumentInfo(order = 4, description = "For private message delivery, show this text with the link")
    private String longDisplayText;

    @ApiArgumentInfo(order = 5, description = "For private message delivery, show this Discord embed for the link")
    private SimpleEmbed longDisplayEmbed;

    @ApiArgumentInfo(order = 6, description = "For push notification delivery, show this short message")
    private String notificationDisplayText;

    @ApiArgumentInfo(order = 7, description = "For automatic workflow delivery, show this short message")
    private String workflowDisplayText;

    public DeliverUrlRequest() { }

    public DeliverUrlRequest(String discordUserId, String url, long ttlSeconds, DeliveryType preferredDeliveryType,
                             String longDisplayText, SimpleEmbed longDisplayEmbed) {
        this.discordUserId = discordUserId;
        this.url = url;
        this.ttlSeconds = ttlSeconds;
        this.preferredDeliveryType = preferredDeliveryType;
        this.longDisplayText = longDisplayText;
        this.longDisplayEmbed = longDisplayEmbed;
    }

    public DeliverUrlRequest(String discordUserId, String url, long ttlSeconds, DeliveryType preferredDeliveryType,
                             String longDisplayText, SimpleEmbed longDisplayEmbed, String notificationDisplayText) {
        this.discordUserId = discordUserId;
        this.url = url;
        this.ttlSeconds = ttlSeconds;
        this.preferredDeliveryType = preferredDeliveryType;
        this.longDisplayText = longDisplayText;
        this.longDisplayEmbed = longDisplayEmbed;
        this.notificationDisplayText = notificationDisplayText;
    }

    public DeliverUrlRequest(String discordUserId, String url, long ttlSeconds, DeliveryType preferredDeliveryType,
                             String longDisplayText, SimpleEmbed longDisplayEmbed, String notificationDisplayText,
                             String workflowDisplayText) {
        this.discordUserId = discordUserId;
        this.url = url;
        this.ttlSeconds = ttlSeconds;
        this.preferredDeliveryType = preferredDeliveryType;
        this.longDisplayText = longDisplayText;
        this.longDisplayEmbed = longDisplayEmbed;
        this.notificationDisplayText = notificationDisplayText;
        this.workflowDisplayText = workflowDisplayText;
    }

    public String getDiscordUserId() {
        return discordUserId;
    }

    public String getUrl() {
        return url;
    }

    public long getTtlSeconds() {
        return ttlSeconds;
    }

    public DeliveryType getPreferredDeliveryType() {
        return preferredDeliveryType;
    }

    public String getLongDisplayText() {
        return longDisplayText;
    }

    public SimpleEmbed getLongDisplayEmbed() {
        return longDisplayEmbed;
    }

    public String getNotificationDisplayText() {
        return notificationDisplayText;
    }

    public String getWorkflowDisplayText() {
        return workflowDisplayText;
    }

}
