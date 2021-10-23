package com.admiralbot.appdaemon.nativesub;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.TargetClass;
import com.oracle.svm.core.jdk.JDK17OrLater;

import java.security.SecureRandom;

// New issue with cached (Secure)Random instance in JDK17, see here: https://github.com/quarkusio/quarkus/issues/19633
@TargetClass(className = "sun.security.jca.JCAUtil", onlyWith = JDK17OrLater.class)
public final class sun_security_jca_JCAUtil {
    
    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset)
    private static SecureRandom def;
}
