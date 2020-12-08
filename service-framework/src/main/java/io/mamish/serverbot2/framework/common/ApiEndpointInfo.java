package io.mamish.serverbot2.framework.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ApiEndpointInfo {
    String serviceName();
    String uriPath(); // Note: this SHOULD have a leading slash
    ApiHttpMethod httpMethod();
    ApiAuthType authType();
}
