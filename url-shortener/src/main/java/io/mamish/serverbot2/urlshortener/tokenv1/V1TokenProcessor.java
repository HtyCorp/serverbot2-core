package io.mamish.serverbot2.urlshortener.tokenv1;

import io.mamish.serverbot2.sharedutil.Pair;
import io.mamish.serverbot2.urlshortener.ITokenProcessor;
import io.mamish.serverbot2.urlshortener.InvalidTokenException;
import io.mamish.serverbot2.urlshortener.UrlRevokedException;
import software.amazon.awssdk.core.SdkBytes;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;

public class V1TokenProcessor implements ITokenProcessor<V1UrlInfoBean> {

    /*
     * Token format: urlsafeb64(ID[8] | KEY[16]))
     *
     * 'ID' is used (after conversion to unsigned long for readability) as sort key for DDB lookup.
     * 'KEY' is the AES128 data key used to encrypt the URL in DDB; without the token the DDB data is unreadable.
     *
     * See V1UrlInfoBean for table storage details.
     */

    private final Base64.Encoder b64encoder = Base64.getUrlEncoder();
    private final Base64.Decoder b64decoder = Base64.getUrlDecoder();
    private final SecureRandom secureRandom = new SecureRandom();
    private final Cipher cipher;
    private final MessageDigest digest;

    public V1TokenProcessor() {
        try {
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            digest = MessageDigest.getInstance("SHA-256");
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Somehow failed to get instance for mandatory cipher/digest", e);
        }
    }

    @Override
    public Pair<String,V1UrlInfoBean> generateTokenAndBean(String url, long ttlSeconds) {

        byte[] plaintextUrlBytes = url.getBytes(StandardCharsets.UTF_8);
        long expiresAtEpochSeconds = Instant.now().plusSeconds(ttlSeconds).toEpochMilli() / 1000;

        // Generate a random ID and data key, and encode them as a token

        long newId = secureRandom.nextLong();
        byte[] dataKey = new byte[16]; secureRandom.nextBytes(dataKey);

        String newToken = encodeIdAndKey(newId, dataKey);

        // Calculate the URL ciphertext and SHA256 digest

        byte[] urlIvBytes, urlCiphertextBytes, urlPlaintextDigestBytes;
        try {
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(dataKey, "AES"));
            urlIvBytes = cipher.getIV();
            urlCiphertextBytes = cipher.doFinal(plaintextUrlBytes);
            digest.reset();
            urlPlaintextDigestBytes = digest.digest(plaintextUrlBytes);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("token encryption failure", e);
        }

        // Generate storage item and return it with the encoded token

        V1UrlInfoBean newInfoBean = new V1UrlInfoBean(1,
                Long.toUnsignedString(newId),
                expiresAtEpochSeconds,
                SdkBytes.fromByteArray(urlIvBytes),
                SdkBytes.fromByteArray(urlCiphertextBytes),
                SdkBytes.fromByteArray(urlPlaintextDigestBytes)
        );
        return new Pair<>(newToken, newInfoBean);

    }

    private String encodeIdAndKey(long newId, byte[] keyBytes) {
        ByteBuffer idAndKeyBuffer = ByteBuffer.allocate(24);
        idAndKeyBuffer.order(ByteOrder.BIG_ENDIAN);
        idAndKeyBuffer.putLong(newId);
        idAndKeyBuffer.put(keyBytes);
        return b64encoder.encodeToString(idAndKeyBuffer.array());
    }

    @Override
    public String extractIdFromToken(String token) {
        try {
            ByteBuffer tokenBuffer = ByteBuffer.wrap(b64decoder.decode(token));
            tokenBuffer.order(ByteOrder.BIG_ENDIAN);
            long id = tokenBuffer.getLong();
            return Long.toUnsignedString(id);
        } catch (RuntimeException e) {
            throw new InvalidTokenException("token id decode", e);
        }
    }

    @Override
    public String extractFullUrlFromTokenAndBean(String token, V1UrlInfoBean details) {

        // Validate bean is still valid

        if (Instant.now().isAfter(Instant.ofEpochSecond(details.getExpiresAtEpochSeconds()))) {
            throw new UrlRevokedException("URL expired");
        }

        // Get decryption inputs: IV from storage and key from user-supplied token

        IvParameterSpec aesIv = new IvParameterSpec(details.getUrlIv().asByteArray());
        Key dataKey;
        try {
            dataKey = extractDataKey(token);
        } catch (RuntimeException e) {
            throw new InvalidTokenException("key extract failure", e);
        }

        // Decrypt full URL ciphertext

        byte[] urlPlaintextBytes;
        try {
            cipher.init(Cipher.DECRYPT_MODE, dataKey, aesIv);
            urlPlaintextBytes = cipher.doFinal(details.getUrlCiphertext().asByteArray());
        } catch (GeneralSecurityException e) {
            throw new InvalidTokenException("url decrypt failure", e);
        }

        // Calculate SHA256 of decrypted URL and compare to stored digest

        digest.reset();
        byte[] actualPlaintextSha256Bytes = digest.digest(urlPlaintextBytes);
        if (!Arrays.equals(actualPlaintextSha256Bytes, details.getUrlSha256().asByteArray())) {
            throw new InvalidTokenException("digest mismatch");
        }

        return new String(urlPlaintextBytes, StandardCharsets.UTF_8);

    }

    private Key extractDataKey(String token) {
        ByteBuffer tokenBuffer = ByteBuffer.wrap(b64decoder.decode(token));
        tokenBuffer.order(ByteOrder.BIG_ENDIAN);
        byte[] dataKeyBytes = new byte[16];
        tokenBuffer.position(8);
        tokenBuffer.get(dataKeyBytes);
        return new SecretKeySpec(dataKeyBytes, "AES");
    }

}
