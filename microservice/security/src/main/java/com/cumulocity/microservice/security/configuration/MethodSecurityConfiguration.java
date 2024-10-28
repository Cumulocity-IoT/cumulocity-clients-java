package com.cumulocity.microservice.security.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.core.GrantedAuthorityDefaults;

/**
 * @see com.cumulocity.microservice.security.service.SecurityExpressionService
 */
@Configuration
@EnableMethodSecurity(jsr250Enabled = true, securedEnabled = true)
public class MethodSecurityConfiguration {

    public static final String ROLE_PREFIX = "ROLE_";

    @Bean
    public GrantedAuthorityDefaults grantedAuthorityDefaults() {
        return new GrantedAuthorityDefaults(ROLE_PREFIX);
    }
}
