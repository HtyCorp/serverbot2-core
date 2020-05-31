package io.mamish.serverbot2.networksecurity.crypto;

import io.mamish.serverbot2.sharedutil.Pair;
import software.amazon.awssdk.core.SdkBytes;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;

public interface ICryptoMaster {

    String encrypt(SdkBytes plaintext);
    SdkBytes decrypt(String ciphertext);
    Pair<SdkBytes,String> generateDataKeyPlaintextAndCiphertext();

}
