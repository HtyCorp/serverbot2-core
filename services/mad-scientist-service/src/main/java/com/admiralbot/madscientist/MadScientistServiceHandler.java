package com.admiralbot.madscientist;

import com.admiralbot.madscientist.model.CountBucketsRequest;
import com.admiralbot.madscientist.model.CountBucketsResponse;
import com.admiralbot.madscientist.model.IMadScientist;
import com.admiralbot.sharedutil.SdkUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;

public class MadScientistServiceHandler implements IMadScientist  {

    private final S3Client s3Client = SdkUtils.client(S3Client.builder());

    @Override
    public CountBucketsResponse countBuckets(CountBucketsRequest request) {
        boolean noNamePrefix = request.getNamePrefix() == null;
        System.out.println(this.s3Client);
        ListBucketsResponse bucketsResponse = s3Client.listBuckets();
        System.out.println(bucketsResponse);
        System.out.println(bucketsResponse.buckets());
        System.out.println(bucketsResponse.buckets().size());
        return new CountBucketsResponse(s3Client.listBuckets().buckets().stream()
                .filter(bucket -> noNamePrefix || bucket.name().startsWith(request.getNamePrefix()))
                .count());
    }

}
