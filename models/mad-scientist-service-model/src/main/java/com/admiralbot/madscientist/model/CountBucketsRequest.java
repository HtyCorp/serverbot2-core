package com.admiralbot.madscientist.model;

import com.admiralbot.framework.common.ApiArgumentInfo;
import com.admiralbot.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 0, name = "CountBuckets", numRequiredFields = 0,
        description = "Count the buckets in this AWS account, optionally with a name prefix filter")
public class CountBucketsRequest {

    @ApiArgumentInfo(order = 0, description = "Only count buckets with a name starting with this string")
    private String namePrefix;

    public CountBucketsRequest() { }

    public CountBucketsRequest(String namePrefix) {
        this.namePrefix = namePrefix;
    }

    public String getNamePrefix() {
        return namePrefix;
    }
}
