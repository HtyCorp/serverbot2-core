package io.mamish.serverbot2.networksecurity;

import io.mamish.serverbot2.sharedconfig.NetSecConfig;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DataKeySpec;
import software.amazon.awssdk.services.kms.model.EncryptResponse;
import software.amazon.awssdk.services.kms.model.GenerateDataKeyWithoutPlaintextResponse;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.Base64;
import java.util.Map;

public class CryptoHelper {

    private static final Map<String,String> STANDARD_ENCRYPTION_CONTEXT = Map.of(
            "service", "NetworkSecurityService"
    );

    private final KmsClient kmsClient = KmsClient.create();
    private final String KEYID = "alias/"+NetSecConfig.KMS_ALIAS;
    private final Base64.Encoder b64Encoder = Base64.getEncoder();
    private final Base64.Decoder b64Decoder = Base64.getDecoder();
    private final Cipher cipher;

    public CryptoHelper() {
        try {
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Somehow failed to get instance for mandatory cipher", e);
        }
    }

    String encrypt(SdkBytes plaintextBytes) {
        EncryptResponse response = kmsClient.encrypt(r -> r.keyId(KEYID)
                .plaintext(plaintextBytes)
                .encryptionContext(STANDARD_ENCRYPTION_CONTEXT));
        byte[] bytesOut = response.ciphertextBlob().asByteArray();
        return b64Encoder.encodeToString(bytesOut);
    }

    SdkBytes decrypt(String ciphertextBase64String) {
        byte[] decoded = b64Decoder.decode(ciphertextBase64String);
        SdkBytes bytesIn = SdkBytes.fromByteArray(decoded);
        return kmsClient.decrypt(r -> r.ciphertextBlob(bytesIn)
                .encryptionContext(STANDARD_ENCRYPTION_CONTEXT)).plaintext();
    }

    String generateDataKey() {
        GenerateDataKeyWithoutPlaintextResponse response = kmsClient.generateDataKeyWithoutPlaintext(r -> r.keyId(KEYID)
                .keySpec(DataKeySpec.AES_128)
                .encryptionContext(STANDARD_ENCRYPTION_CONTEXT));
        byte[] bytesOut = response.ciphertextBlob().asByteArray();
        return b64Encoder.encodeToString(bytesOut);
    }

    Key decryptDataKey(String ciphertextBase64String) {
        byte[] keyBytes = decrypt(ciphertextBase64String).asByteArray();
        return new SecretKeySpec(keyBytes, "AES");
    }

    String encryptLocal(SdkBytes plaintext, Key dataKey) {
        try {
            cipher.init(Cipher.ENCRYPT_MODE, dataKey);
            byte[] ciphertextBytes = cipher.doFinal(plaintext.asByteArray());
            return b64Encoder.encodeToString(ciphertextBytes);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Cipher error during local encryption", e);
        }
    }

    SdkBytes decryptLocal(String ciphertextBase64String, Key dataKey) {
        try {
            byte[] ciphertextBytes = b64Decoder.decode(ciphertextBase64String);
            cipher.init(Cipher.DECRYPT_MODE, dataKey);
            return SdkBytes.fromByteArray(cipher.doFinal(ciphertextBytes));
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Cipher error during local decryption", e);
        }
    }

}
