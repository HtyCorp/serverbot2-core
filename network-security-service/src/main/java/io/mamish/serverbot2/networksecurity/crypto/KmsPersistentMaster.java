package io.mamish.serverbot2.networksecurity.crypto;

import io.mamish.serverbot2.sharedconfig.NetSecConfig;
import io.mamish.serverbot2.sharedutil.Pair;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DataKeySpec;
import software.amazon.awssdk.services.kms.model.GenerateDataKeyResponse;

import java.util.Base64;
import java.util.Map;

public class KmsPersistentMaster implements ICryptoMaster {

    private static final Map<String,String> STANDARD_ENCRYPTION_CONTEXT = Map.of(
            "service", "NetworkSecurityService"
    );

    private final KmsClient kmsClient = KmsClient.builder()
            .httpClient(UrlConnectionHttpClient.create())
            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
            .build();
    private final String KEYID = "alias/"+ NetSecConfig.KMS_ALIAS;
    private final Base64.Encoder b64Encoder = Base64.getEncoder();
    private final Base64.Decoder b64Decoder = Base64.getDecoder();

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
