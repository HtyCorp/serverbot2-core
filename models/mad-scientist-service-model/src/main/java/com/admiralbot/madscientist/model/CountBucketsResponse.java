package com.admiralbot.madscientist.model;

public class CountBucketsResponse {

    private long numBuckets;

    public CountBucketsResponse() { }

    public CountBucketsResponse(long numBuckets) {
        this.numBuckets = numBuckets;
    }

    public long getNumBuckets() {
        return numBuckets;
    }
}
