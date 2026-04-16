package com.cumulocity.microservice.security.annotation;

import java.lang.annotation.*;

/**
 * Marks a REST endpoint as deliberately unsecured and exempt from security validation checks.
 *
 * <p>Use this on endpoint methods (or an entire controller class) to signal that the endpoint
 * intentionally requires no authentication/authorization, and to suppress the build-time
 * {@code validate-rest-security} plugin check.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * &#64;RestController
 * public class HealthController {
 *
 *     &#64;UnauthorizedEndpoint("Public health check")
 *     &#64;GetMapping("/health")
 *     public ResponseEntity&lt;String&gt; health() {
 *         return ResponseEntity.ok("OK");
 *     }
 * }
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface UnauthorizedEndpoint {
    /**
     * Optional description explaining why this endpoint is unsecured.
     */
    String value() default "";
}

