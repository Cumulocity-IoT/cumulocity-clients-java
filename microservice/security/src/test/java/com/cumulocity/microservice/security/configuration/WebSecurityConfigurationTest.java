package com.cumulocity.microservice.security.configuration;

import com.cumulocity.microservice.context.annotation.EnableContextSupportConfiguration;
import com.cumulocity.microservice.monitoring.actuator.annotation.EnableActuatorDefaultConfiguration;
import com.cumulocity.microservice.monitoring.health.annotation.EnableHealthIndicator;
import com.cumulocity.microservice.security.token.JwtAuthenticatedTokenCache;
import com.cumulocity.microservice.security.token.JwtTokenAuthentication;
import com.cumulocity.microservice.security.token.JwtTokenAuthenticationLoader;
import com.cumulocity.microservice.subscription.service.MicroserviceSubscriptionsService;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {
        WebSecurityConfigurationTest.TestConfig.class,
        EnableContextSupportConfiguration.class,
        WebSecurityConfiguration.class
})
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "management.health.platform.enabled=true"
})
public class WebSecurityConfigurationTest {

    @TestConfiguration
    @EnableAutoConfiguration
    @EnableActuatorDefaultConfiguration
    @EnableHealthIndicator
    static class TestConfig {

        @Bean
        public UserDetailsService testUserDetailsService() {
            return new InMemoryUserDetailsManager(
                    User.withUsername("user").password("user").roles("USER").build(),
                    User.withUsername("admin").password("admin").roles("TENANT_MANAGEMENT_ADMIN").build(),
                    User.withUsername("tenantAdmin").password("admin").roles("TENANT_ADMIN").build()
            );
        }

        @Bean
        public JwtAuthenticatedTokenCache jwtAuthenticatedTokenCache() {
            return (key, loader) -> sneakyCall(loader);
        }

        @SneakyThrows
        JwtTokenAuthentication sneakyCall(JwtTokenAuthenticationLoader loader) {
            return loader.call();
        }
    }

    @MockBean
    MicroserviceSubscriptionsService microserviceSubscriptionsService;

    @Autowired
    MockMvc mvc;

    @Test
    void returnsNoLoggersInfoWhenUnauthenticated() throws Exception {
        mvc.perform(get("/loggers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsLoggersWhenAuthenticated() throws Exception {
        mvc.perform(get("/loggers").with(httpBasic("user", "user")))
                .andExpect(status().isOk());
    }

    @Test
    void allowsLoggersChangesAsTenantAdmin() throws Exception {
        mvc.perform(post("/loggers/c8y").with(csrf()).contentType("application/json").content("{\"configuredLevel\": \"TRACE\"}")
                 .with(httpBasic("admin", "admin")))
                 .andExpect(status().is2xxSuccessful());

        mvc.perform(get("/loggers/c8y").with(httpBasic("user", "user")))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("configuredLevel").value("TRACE"));
    }

    @Test
    void doesNotAllowLoggersChangesAsAuthenticatedButNotAuthorizedUser() throws Exception {
        mvc.perform(post("/loggers").with(httpBasic("user", "user")))
                .andExpect(status().isForbidden());
    }

    @Test
    void returnsHealthWithNoDetailsWhenUnauthenticated() throws Exception {
        mvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("status").value("UP"))
                .andExpect(jsonPath("components").doesNotExist());
    }

    @Test
    void returnsHealthWithDetailsWhenUserAuthenticated() throws Exception {
        mvc.perform(get("/health").with(httpBasic("user", "user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("status").value("UP"))
                .andExpect(jsonPath("components").exists());
    }
    @Test
    void returnsHealthWithDetailsWhenAdminAuthenticated() throws Exception {
        mvc.perform(get("/health").with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("status").value("UP"))
                .andExpect(jsonPath("components").exists());
    }
}
