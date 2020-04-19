package io.mamish.serverbot2.sharedutil.reflect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ApiRequestInfo {
    int order();
    String name();
    String description();
    int numRequiredFields();
}
