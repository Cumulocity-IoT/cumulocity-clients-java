package com.cumulocity.microservice.security.configuration;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

public interface MicroserviceWebSecurityCustomizer extends Customizer<HttpSecurity> {
}
