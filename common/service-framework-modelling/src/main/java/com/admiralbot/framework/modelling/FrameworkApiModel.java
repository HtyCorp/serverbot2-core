package com.admiralbot.framework.modelling;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE})
public @interface FrameworkApiModel {
    // Marker interface to ensure correct reflection setup for native-image
}
