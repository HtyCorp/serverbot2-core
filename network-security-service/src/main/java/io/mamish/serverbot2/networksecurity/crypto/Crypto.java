package io.mamish.serverbot2.networksecurity.crypto;

import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedutil.Pair;
import software.amazon.awssdk.core.SdkBytes;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;

public class Crypto {

    private final ICryptoMaster masterProvider = chooseMasterProvider();
    private final LocalAes localAes = new LocalAes();

    public String encrypt(SdkBytes plaintext) {
        return masterProvider.encrypt(plaintext);
    }

    public SdkBytes decrypt(String ciphertext) {
        return masterProvider.decrypt(ciphertext);
    }

    public Pair<Key,String> generateDataKey() {
        Pair<SdkBytes,String> generated = masterProvider.generateDataKeyPlaintextAndCiphertext();
        Key dataKey = new SecretKeySpec(generated.fst().asByteArray(), "AES");
        String dataKeyCiphertext = generated.snd();
        return new Pair<>(dataKey, dataKeyCiphertext);
    }

    public Key decryptDataKey(String ciphertext) {
        byte[] keyBytes = masterProvider.decrypt(ciphertext).asByteArray();
        return new SecretKeySpec(keyBytes, "AES");
    }

    public String encryptLocal(SdkBytes plaintext, Key dataKey) {
        return localAes.encryptWithDataKey(plaintext, dataKey);
    }

    public SdkBytes decryptLocal(String ciphertext, Key dataKey) {
        return localAes.decryptWithDataKey(ciphertext, dataKey);
    }

    private ICryptoMaster chooseMasterProvider() {
        if (CommonConfig.ENABLE_MOCK.notNull()) {
            return new LocalSessionMaster();
        } else {
            return new KmsMaster();
        }
    }

}
