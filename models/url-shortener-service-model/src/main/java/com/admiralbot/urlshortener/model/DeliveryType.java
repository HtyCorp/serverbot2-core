package com.admiralbot.urlshortener.model;

public enum DeliveryType {
    PRIVATE_MESSAGE_LINK, // URL delivered via Discord private message
    PUSH_NOTIFICATION, // URL delivered as a clickable browser push notification
    AUTOMATIC_WORKFLOW // URL activated automatically without secondary user interaction
}
