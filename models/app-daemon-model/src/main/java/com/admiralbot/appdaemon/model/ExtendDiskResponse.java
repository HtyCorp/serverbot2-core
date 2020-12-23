package com.admiralbot.appdaemon.model;

public class ExtendDiskResponse {

    private String rootPartitionName;
    private String modifiedSize;

    public ExtendDiskResponse() {}

    public ExtendDiskResponse(String rootPartitionName, String modifiedSize) {
        this.rootPartitionName = rootPartitionName;
        this.modifiedSize = modifiedSize;
    }

    public String getRootPartitionName() {
        return rootPartitionName;
    }

    public String getModifiedSize() {
        return modifiedSize;
    }

}
