package com.cumulocity.microservice.settings;

import com.cumulocity.microservice.context.ContextService;
import com.cumulocity.microservice.context.annotation.EnableContextSupportConfiguration;
import com.cumulocity.microservice.context.credentials.MicroserviceCredentials;
import com.cumulocity.microservice.settings.service.MicroserviceSettingsService;
import com.cumulocity.rest.representation.tenant.OptionsRepresentation;
import com.cumulocity.sdk.client.RestOperations;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import jakarta.ws.rs.core.MediaType;

import java.util.Map;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.doReturn;

@Slf4j
@SpringBootTest(classes = {
        EnableContextSupportConfiguration.class,
        EnableTenantOptionSettingsTestConfiguration.class
})
public class EnableTenantOptionSettingsTest {

    @Autowired
    private Environment environment;
    @Autowired
    private RestOperations restOperations;
    @Autowired
    private MicroserviceSettingsService microserviceSettingsService;
    @Autowired
    private ContextService<MicroserviceCredentials> contextService;

    @Test
    public void mustTenantOptionsBeAvailableByService() {
        // given
        OptionsRepresentation options = OptionsRepresentation.builder()
                .property("option1", "value1")
                .property("option2", "value2")
                .build();
        doReturn(options).when(restOperations).get(anyString(), any(MediaType.class), eq(OptionsRepresentation.class));
        // when
        Map<String, String> settings = contextService.callWithinContext(context("t500+"), new Callable<Map<String, String>>() {
            public Map<String, String> call() {
                return microserviceSettingsService.getAll();
            }
        });
        // then
        assertThat(settings).containsOnlyKeys("option1", "option2").containsValues("value1", "value2");
    }

    @Test
    public void mustTenantOptionsBeAvailableByEnvironment() {
        // given
        OptionsRepresentation options = OptionsRepresentation.builder()
                .property("option3", "value31")
                .property("option4", "value40")
                .build();
        doReturn(options).when(restOperations).get(anyString(), any(MediaType.class), eq(OptionsRepresentation.class));
        // when
        String result = contextService.callWithinContext(context("t100"), new Callable<String>() {
            public String call() {
                return environment.getProperty("option3");
            }
        });
        // then
        assertThat(result).isEqualTo("value31");
    }

    @Test
    public void mustGetTenantOptionWhenGetCredential() {
        // given
        OptionsRepresentation options = OptionsRepresentation.builder()
                .property("credentials.password1", "pa$$w0rd")
                .build();
        doReturn(options).when(restOperations).get(anyString(), any(MediaType.class), eq(OptionsRepresentation.class));
        // when
        String password1 = contextService.callWithinContext(context("t1000"), new Callable<String>() {
            public String call() {
                return microserviceSettingsService.getCredential("password1");
            }
        });
        // then
        assertThat(password1).isEqualTo("pa$$w0rd");
    }

    private MicroserviceCredentials context(String tenant) {
        return MicroserviceCredentials.builder().tenant(tenant).username("service_app").build();
    }

}
