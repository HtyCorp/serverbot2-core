package com.admiralbot.echoservice.model;

import com.admiralbot.framework.modelling.ApiArgumentInfo;
import com.admiralbot.framework.modelling.ApiRequestInfo;

@ApiRequestInfo(order = 1, name = "HowMuchStorage", numRequiredFields = 1, description = "Get some storage stats")
public class HowMuchStorageRequest {

    @ApiArgumentInfo(order = 0, description = "What type of storage to look at")
    private StorageType storageType;

    public HowMuchStorageRequest() {}

    public HowMuchStorageRequest(StorageType storageType) {
        this.storageType = storageType;
    }

    public StorageType getStorageType() {
        return storageType;
    }
}
