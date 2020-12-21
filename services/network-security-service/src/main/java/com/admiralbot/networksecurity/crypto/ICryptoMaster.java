package com.admiralbot.networksecurity.crypto;

import com.admiralbot.sharedutil.Pair;
import software.amazon.awssdk.core.SdkBytes;

public interface ICryptoMaster {

    String encrypt(SdkBytes plaintext);
    SdkBytes decrypt(String ciphertext);
    Pair<SdkBytes,String> generateDataKeyPlaintextAndCiphertext();

}
