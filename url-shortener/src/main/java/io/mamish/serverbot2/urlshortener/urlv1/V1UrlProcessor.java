package io.mamish.serverbot2.urlshortener.urlv1;

import io.mamish.serverbot2.sharedutil.Pair;
import io.mamish.serverbot2.urlshortener.IUrlProcessor;
import io.mamish.serverbot2.urlshortener.InvalidTokenException;
import io.mamish.serverbot2.urlshortener.UrlRevokedException;

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

public class V1UrlProcessor implements IUrlProcessor<V1UrlInfoBean> {

    /*
     * Token format: urlsafeb64(ID[8] | KEY[16]))
     */

    private final Base64.Encoder b64encoder = Base64.getUrlEncoder();
    private final Base64.Decoder b64decoder = Base64.getUrlDecoder();
    private final SecureRandom secureRandom = new SecureRandom();
    private final Cipher cipher;
    private final MessageDigest digest;

    public V1UrlProcessor() {
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

        // Generate a random ID and data key, and encode them as the token

        long newId = secureRandom.nextLong();
        Pair<Key,byte[]> dataKey = generateDataKey();

        String newToken = encodeIdAndKey(newId, dataKey.b());

        // Calculate the URL ciphertext and SHA256 digest

        byte[] urlIvBytes, urlCiphertextBytes, urlPlaintextDigestBytes;

        try {
            cipher.init(Cipher.ENCRYPT_MODE, dataKey.a());
            urlIvBytes = cipher.getIV();
            urlCiphertextBytes = cipher.doFinal(plaintextUrlBytes);
            digest.reset();
            urlPlaintextDigestBytes = digest.digest(plaintextUrlBytes);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("token encryption failure", e);
        }

        // Generate storage details and return with the encoded token

        V1UrlInfoBean newInfoBean = new V1UrlInfoBean(1,
                Long.toUnsignedString(newId),
                expiresAtEpochSeconds,
                ByteBuffer.wrap(urlIvBytes),
                ByteBuffer.wrap(urlCiphertextBytes),
                ByteBuffer.wrap(urlPlaintextDigestBytes));
        return new Pair<>(newToken, newInfoBean);

    }

    private Pair<Key,byte[]> generateDataKey() {
        byte[] dataKeyBytes = new byte[16];
        secureRandom.nextBytes(dataKeyBytes);
        Key key = new SecretKeySpec(dataKeyBytes, "AES");
        return new Pair<>(key, dataKeyBytes);
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
        Key dataKey;
        try {
            dataKey = extractDataKey(token);
        } catch (RuntimeException e) {
            throw new InvalidTokenException("key extract failure", e);
        }

        if (Instant.now().isAfter(Instant.ofEpochSecond(details.getExpiresAtEpochSeconds()))) {
            throw new UrlRevokedException("URL expired");
        }

        // Decrypt ciphertext and calculate SHA256 digest of plaintext

        byte[] urlPlaintextBytes;
        try {
            cipher.init(Cipher.DECRYPT_MODE, dataKey, new IvParameterSpec(details.getUrlIv().array()));
            urlPlaintextBytes = cipher.doFinal(details.getUrlCiphertext().array());
        } catch (GeneralSecurityException e) {
            throw new InvalidTokenException("url decrypt failure", e);
        }

        digest.reset();
        byte[] actualPlaintextSha256Bytes = digest.digest(urlPlaintextBytes);
        if (!Arrays.equals(actualPlaintextSha256Bytes, details.getUrlSha256().array())) {
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
