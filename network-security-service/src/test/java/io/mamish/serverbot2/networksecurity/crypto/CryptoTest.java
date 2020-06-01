package io.mamish.serverbot2.networksecurity.crypto;

import io.mamish.serverbot2.sharedutil.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkBytes;

import java.security.Key;
import java.util.List;

public class CryptoTest {

    private final List<String> plaintextStrings = List.of(
            "This is some plaintext.",
            "A really loooooooooooooooooooooooooooong string that's really long.",
            // Ref: https://docs.oracle.com/javase/tutorial/i18n/text/string.html
            new String("A" + "\u00ea" + "\u00f1" + "\u00fc" + "C")
    );

    private Crypto crypto;

    @BeforeEach
    void setUp() {
        crypto = new Crypto();
    }

    @Test
    void testDirectEncrypt() {

        plaintextStrings.forEach(plaintext -> {

            String ciphertext = crypto.encrypt(SdkBytes.fromUtf8String(plaintext));
            String recovered = crypto.decrypt(ciphertext).asUtf8String();

            Assertions.assertNotEquals(plaintext, ciphertext);
            Assertions.assertEquals(plaintext, recovered);

        });

    }

    @Test
    void testEnvelopeEncrypt() {

        plaintextStrings.forEach(plaintext -> {

            SdkBytes plaintextBytes = SdkBytes.fromUtf8String(plaintext);

            Pair<Key,String> generated = crypto.generateDataKey();
            Key dataKey = generated.a();
            String dataKeyCiphertext = generated.b();
            Key dataKeyRecovered = crypto.decryptDataKey(dataKeyCiphertext);

            Assertions.assertEquals(dataKey, dataKeyRecovered);

            String ciphertext1 = crypto.encryptLocal(plaintextBytes, dataKey);
            String ciphertext2 = crypto.encryptLocal(plaintextBytes, dataKeyRecovered);

            String recovered1a = crypto.decryptLocal(ciphertext1, dataKey).asUtf8String();
            String recovered1b = crypto.decryptLocal(ciphertext1, dataKeyRecovered).asUtf8String();
            String recovered2a = crypto.decryptLocal(ciphertext2, dataKey).asUtf8String();
            String recovered2b = crypto.decryptLocal(ciphertext2, dataKeyRecovered).asUtf8String();

            Assertions.assertEquals(plaintext, recovered1a);
            Assertions.assertEquals(plaintext, recovered1b);
            Assertions.assertEquals(plaintext, recovered2a);
            Assertions.assertEquals(plaintext, recovered2b);

        });

    }
}
