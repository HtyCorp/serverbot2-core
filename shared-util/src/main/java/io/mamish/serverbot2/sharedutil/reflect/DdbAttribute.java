package io.mamish.serverbot2.sharedutil.reflect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DdbAttribute {
    String value();
    DdbKeyType keyType() default DdbKeyType.NONE;
}
