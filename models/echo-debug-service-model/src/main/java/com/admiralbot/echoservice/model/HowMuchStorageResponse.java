package com.admiralbot.echoservice.model;

public class HowMuchStorageResponse {
    private StorageType storageType;
    private int numberOfObjects;
    private int totalStorageGb;

    public HowMuchStorageResponse() {}

    public HowMuchStorageResponse(StorageType storageType, int numberOfObjects, int totalStorageGb) {
        this.storageType = storageType;
        this.numberOfObjects = numberOfObjects;
        this.totalStorageGb = totalStorageGb;
    }

    public StorageType getStorageType() {
        return storageType;
    }

    public int getNumberOfObjects() {
        return numberOfObjects;
    }

    public int getTotalStorageGb() {
        return totalStorageGb;
    }
}
