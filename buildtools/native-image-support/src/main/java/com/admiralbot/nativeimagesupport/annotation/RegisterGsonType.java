package com.admiralbot.nativeimagesupport.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE})
public @interface RegisterGsonType {
    Class<?>[] types() default {};
    boolean includeThis() default false;
}
