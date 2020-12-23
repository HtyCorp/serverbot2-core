package com.admiralbot.appdaemon.model;

import com.admiralbot.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 3, name = "ExtendDisk", numRequiredFields = 0,
        description = "Grow the root partition to match its block device (i.e. after expanding the underlying EBS volume)")
public class ExtendDiskRequest {

    // No parameters required

    public ExtendDiskRequest() {}

}
