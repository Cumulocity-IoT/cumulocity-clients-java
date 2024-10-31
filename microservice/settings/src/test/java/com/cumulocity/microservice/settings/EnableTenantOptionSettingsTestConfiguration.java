package com.cumulocity.microservice.settings;

import com.cumulocity.microservice.context.ContextService;
import com.cumulocity.microservice.context.ContextServiceImpl;
import com.cumulocity.microservice.context.credentials.MicroserviceCredentials;
import com.cumulocity.microservice.settings.annotation.EnableTenantOptionSettingsConfiguration;
import com.cumulocity.microservice.subscription.model.core.PlatformProperties;
import com.cumulocity.rest.representation.tenant.OptionsRepresentation;
import com.cumulocity.sdk.client.RestOperations;
import com.google.common.base.Suppliers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@Configuration
public class EnableTenantOptionSettingsTestConfiguration extends EnableTenantOptionSettingsConfiguration {

    @Bean
    public RestOperations restOperations() {
        RestOperations restOperations = mock(RestOperations.class);
        doReturn(new OptionsRepresentation())
                .when(restOperations)
                .get("http://fake_platform:8111/application/currentApplication/settings", APPLICATION_JSON_TYPE, OptionsRepresentation.class);
        return restOperations;
    }

    @Bean
    public ContextService<MicroserviceCredentials> contextService () {
        return new ContextServiceImpl<>(MicroserviceCredentials.class);
    }

    @Bean
    public PlatformProperties platformProperties() {
        return PlatformProperties.builder()
                .url(Suppliers.ofInstance("http://fake_platform:8111"))
                .microserviceBoostrapUser(MicroserviceCredentials.builder()
                    .tenant("t666")
                    .username("servicebootstrap_app")
                    .build())
                .build();
    }
}

