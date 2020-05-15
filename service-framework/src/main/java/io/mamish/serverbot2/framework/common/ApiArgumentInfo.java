package io.mamish.serverbot2.framework.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ApiArgumentInfo {
    int order();
    String name(); // TODO: Remove this; it's redundant over actual field name. Need to refactor ApiActionDefinition for change.
    String description();
}
