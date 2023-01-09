package com.cumulocity.microservice.security.annotation;

import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that all the exposed endpoints other than /metadata requires
 * base64 authorization and authentication.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({
        TokenCacheConfiguration.class,
        ErrorControllerConfiguration.class,
        EnableGlobalMethodSecurityConfiguration.class,
        EnableWebSecurityConfiguration.class
})
public @interface EnableMicroserviceSecurity {
}
