package com.admiralbot.urlshortener.model;

public class DeliverUrlResponse {

    private DeliveryType deliveryTypeUsed;

    public DeliverUrlResponse() { }

    public DeliverUrlResponse(DeliveryType deliveryTypeUsed) {
        this.deliveryTypeUsed = deliveryTypeUsed;
    }

    public DeliveryType getDeliveryTypeUsed() {
        return deliveryTypeUsed;
    }

}
