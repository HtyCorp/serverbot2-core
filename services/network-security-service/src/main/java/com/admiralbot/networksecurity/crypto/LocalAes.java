package com.admiralbot.networksecurity.crypto;

import com.admiralbot.sharedutil.Joiner;
import software.amazon.awssdk.core.SdkBytes;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.Base64;

public class LocalAes {

    private final Base64.Encoder b64Encoder = Base64.getEncoder();
    private final Base64.Decoder b64Decoder = Base64.getDecoder();

    private final Cipher cipher;

    public LocalAes() {
        try {
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Somehow failed to get instance for mandatory cipher", e);
        }
    }

    String encryptWithDataKey(SdkBytes plaintext, Key dataKey) {
        try {
            cipher.init(Cipher.ENCRYPT_MODE, dataKey);

            byte[] ivBytes = cipher.getIV();
            byte[] ciphertextBytes = cipher.doFinal(plaintext.asByteArray());

            String ivB64 = b64Encoder.encodeToString(ivBytes);
            String ciphertextB64 = b64Encoder.encodeToString(ciphertextBytes);

            return Joiner.colon(ivB64, ciphertextB64);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Cipher error during local encryption", e);
        }
    }

    SdkBytes decryptWithDataKey(String ivAndCiphertextBase64, Key dataKey) {
        try {
            String ivB64 = ivAndCiphertextBase64.split(":")[0];
            String ciphertextB64 = ivAndCiphertextBase64.split(":")[1];

            byte[] ivBytes = b64Decoder.decode(ivB64);
            byte[] ciphertextBytes = b64Decoder.decode(ciphertextB64);

            cipher.init(Cipher.DECRYPT_MODE, dataKey, new IvParameterSpec(ivBytes));
            return SdkBytes.fromByteArray(cipher.doFinal(ciphertextBytes));
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Cipher error during local decryption", e);
        }
    }

}
