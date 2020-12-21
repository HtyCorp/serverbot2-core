package com.admiralbot.urlshortener.tokenv1;

import com.admiralbot.urlshortener.BaseUrlInfoBean;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@DynamoDbBean
public class V1UrlInfoBean extends BaseUrlInfoBean {

    /*
     * This bean stores an encrypted version of the requested URL. The data key used for this is encoded in the user
     * token forming the shortened URL. So, even with access to this table, the URLs are inaccessible with their tokens.
     *
     * See V1UrlProcessor for token details.
     *
     * SdkBytes is used for binary representation since it is mappable by Enhanced DDB client and is flexible.
     */

    private long expiresAtEpochSeconds;
    private SdkBytes urlIv;
    private SdkBytes urlCiphertext;
    private SdkBytes urlSha256;

    public V1UrlInfoBean() { }

    public V1UrlInfoBean(int schemaVersion, String id, long expiresAtEpochSeconds, SdkBytes urlIv,
                         SdkBytes urlCiphertext, SdkBytes urlSha256) {
        super(schemaVersion, id);
        this.expiresAtEpochSeconds = expiresAtEpochSeconds;
        this.urlIv = urlIv;
        this.urlCiphertext = urlCiphertext;
        this.urlSha256 = urlSha256;
    }

    public long getExpiresAtEpochSeconds() {
        return expiresAtEpochSeconds;
    }

    public void setExpiresAtEpochSeconds(long expiresAtEpochSeconds) {
        this.expiresAtEpochSeconds = expiresAtEpochSeconds;
    }

    public SdkBytes getUrlIv() {
        return urlIv;
    }

    public void setUrlIv(SdkBytes urlIv) {
        this.urlIv = urlIv;
    }

    public SdkBytes getUrlCiphertext() {
        return urlCiphertext;
    }

    public void setUrlCiphertext(SdkBytes urlCiphertext) {
        this.urlCiphertext = urlCiphertext;
    }

    public SdkBytes getUrlSha256() {
        return urlSha256;
    }

    public void setUrlSha256(SdkBytes urlSha256) {
        this.urlSha256 = urlSha256;
    }

    @Override
    public String toString() {
        return "V1UrlInfoBean{" +
                "expiresAtEpochSeconds=" + expiresAtEpochSeconds +
                ", urlIv=" + urlIv +
                ", urlCiphertext=" + urlCiphertext +
                ", urlSha256=" + urlSha256 +
                ", schemaVersion=" + schemaVersion +
                ", id='" + id + '\'' +
                '}';
    }
}
