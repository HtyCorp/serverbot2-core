package io.mamish.serverbot2.urlshortener.urlv1;

import io.mamish.serverbot2.urlshortener.BaseUrlInfoBean;

import java.nio.ByteBuffer;

public class V1UrlInfoBean extends BaseUrlInfoBean {

    /*
     * This bean stores an encrypted version of the requested URL. The data key used for this is encoded in the user
     * token forming the shortened URL. So, even with access to this table, the URLs are inaccessible with their tokens.
     *
     * See V1UrlProcessor for token details.
     */

    private long expiresAtEpochSeconds;
    private ByteBuffer urlIv;
    private ByteBuffer urlCiphertext;
    private ByteBuffer urlSha256;

    public V1UrlInfoBean() { }

    public V1UrlInfoBean(int schemaVersion, String id, long expiresAtEpochSeconds, ByteBuffer urlIv,
                         ByteBuffer urlCiphertext, ByteBuffer urlSha256) {
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

    public ByteBuffer getUrlIv() {
        return urlIv;
    }

    public void setUrlIv(ByteBuffer urlIv) {
        this.urlIv = urlIv;
    }

    public ByteBuffer getUrlCiphertext() {
        return urlCiphertext;
    }

    public void setUrlCiphertext(ByteBuffer urlCiphertext) {
        this.urlCiphertext = urlCiphertext;
    }

    public ByteBuffer getUrlSha256() {
        return urlSha256;
    }

    public void setUrlSha256(ByteBuffer urlSha256) {
        this.urlSha256 = urlSha256;
    }
}
