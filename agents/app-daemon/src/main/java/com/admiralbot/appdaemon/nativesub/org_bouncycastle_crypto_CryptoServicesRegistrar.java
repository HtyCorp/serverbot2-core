package com.admiralbot.appdaemon.nativesub;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.TargetClass;

import java.security.SecureRandom;

/**
 * Images can't contain Random instances because they would always have the same seed (compiler specifically detects
 * this and fails compilation), but due to our usage of BouncyCastle, which has to be mostly initialized at build time,
 * some Random instances are created at build time.
 *
 * With our current build-time configuration, the *only* active Random instance is this one:
 * https://github.com/bcgit/bc-java/blob/28874e81b6e92c923f93900ddea9a726451f946e/core/src/main/java/org/bouncycastle/crypto/CryptoServicesRegistrar.java#L32
 *
 * Other classes fetch this instance via a public static method, which initializes it if null. Therefore, we can solve
 * the problem by forcing the field to be reset to null in the image, so at runtime it will be re-created.
 */
@TargetClass(org.bouncycastle.crypto.CryptoServicesRegistrar.class)
public final class org_bouncycastle_crypto_CryptoServicesRegistrar {

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset)
    private static SecureRandom defaultSecureRandom;
}
