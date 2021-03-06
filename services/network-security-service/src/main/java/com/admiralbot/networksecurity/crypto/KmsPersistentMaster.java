package com.admiralbot.networksecurity.crypto;

import com.admiralbot.sharedconfig.NetSecConfig;
import com.admiralbot.sharedutil.Pair;
import com.admiralbot.sharedutil.SdkUtils;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DataKeySpec;
import software.amazon.awssdk.services.kms.model.GenerateDataKeyResponse;

import java.util.Base64;
import java.util.Map;

public class KmsPersistentMaster implements ICryptoMaster {

    private static final Map<String,String> STANDARD_ENCRYPTION_CONTEXT = Map.of(
            "service", "NetworkSecurityService"
    );

    private final KmsClient kmsClient = SdkUtils.client(KmsClient.builder());
    private final String KEYID = "alias/"+ NetSecConfig.KMS_ALIAS;
    private final Base64.Encoder b64Encoder = Base64.getUrlEncoder();
    private final Base64.Decoder b64Decoder = Base64.getUrlDecoder();

    @Override
    public String encrypt(SdkBytes plaintext) {
        SdkBytes ciphertextBlob = kmsClient.encrypt(r -> r.keyId(KEYID)
                .plaintext(plaintext)
                .encryptionContext(STANDARD_ENCRYPTION_CONTEXT)
        ).ciphertextBlob();
        byte[] ciphertextBytes = ciphertextBlob.asByteArray();
        return b64Encoder.encodeToString(ciphertextBytes);
    }

    @Override
    public SdkBytes decrypt(String ciphertext) {
        byte[] ciphertextBytes = b64Decoder.decode(ciphertext);
        SdkBytes ciphertextBlob = SdkBytes.fromByteArray(ciphertextBytes);
        return kmsClient.decrypt(r -> r.ciphertextBlob(ciphertextBlob)
                .encryptionContext(STANDARD_ENCRYPTION_CONTEXT)
        ).plaintext();
    }

    @Override
    public Pair<SdkBytes,String> generateDataKeyPlaintextAndCiphertext() {
        GenerateDataKeyResponse response = kmsClient.generateDataKey(r -> r.keyId(KEYID)
                .keySpec(DataKeySpec.AES_128)
                .encryptionContext(STANDARD_ENCRYPTION_CONTEXT));
        SdkBytes dataKeyPlaintext = response.plaintext();
        String dataKeyCiphertext = b64Encoder.encodeToString(response.ciphertextBlob().asByteArray());
        return new Pair<>(dataKeyPlaintext, dataKeyCiphertext);
    }
}
