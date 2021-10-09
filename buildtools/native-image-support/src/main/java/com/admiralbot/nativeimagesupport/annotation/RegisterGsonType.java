package com.admiralbot.nativeimagesupport.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target({
        ElementType.TYPE,
        ElementType.FIELD,
        ElementType.LOCAL_VARIABLE,
        ElementType.PARAMETER
})
public @interface RegisterGsonType {
    // Marker interface to indicate this type (or the type of this field) needs a preloaded Gson TypeAdapter.
    // The adapters are registered in the Gson instance accessible via ImageCache.getGson().
}
