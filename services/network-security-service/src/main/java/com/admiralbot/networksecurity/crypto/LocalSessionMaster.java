package com.admiralbot.networksecurity.crypto;

import com.admiralbot.sharedutil.Pair;
import software.amazon.awssdk.core.SdkBytes;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.SecureRandom;

/**
 * A dummy implementation of crypto master that does not store data securely.
 * <p>
 * This re-uses the existing LocalCrypto class with a randomly generated in-memory key.
 */
public class LocalSessionMaster implements ICryptoMaster {

    private final LocalAes localAes = new LocalAes();
    private final SecureRandom secureRandom = new SecureRandom();
    private final Key sessionMasterKey = new SecretKeySpec(randomAes128Bytes(), "AES");

    @Override
    public String encrypt(SdkBytes plaintext) {
        return localAes.encryptWithDataKey(plaintext, sessionMasterKey);
    }

    @Override
    public SdkBytes decrypt(String ciphertext) {
        return localAes.decryptWithDataKey(ciphertext, sessionMasterKey);
    }

    @Override
    public Pair<SdkBytes, String> generateDataKeyPlaintextAndCiphertext() {
        SdkBytes plaintext = SdkBytes.fromByteArray(randomAes128Bytes());
        String ciphertext = localAes.encryptWithDataKey(plaintext, sessionMasterKey);
        return new Pair<>(plaintext, ciphertext);
    }

    private byte[] randomAes128Bytes() {
        byte[] randomBytes = new byte[16];
        secureRandom.nextBytes(randomBytes);
        return randomBytes;
    }

}
