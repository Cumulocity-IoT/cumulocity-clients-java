package com.cumulocity.microservice.monitoring.health.controller.configuration;

import com.cumulocity.microservice.context.annotation.EnableContextSupport;
import com.cumulocity.microservice.subscription.model.core.PlatformProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.mockito.Mockito.mock;

@Configuration
@EnableContextSupport
public class TestPlatformConfiguration {
    @Bean
    public PlatformProperties platformProperties() {
        return mock(PlatformProperties.class);
    }
}
