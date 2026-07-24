package com.cumulocity.microservice.context.annotation;

import com.cumulocity.microservice.context.ContextService;
import com.cumulocity.microservice.context.credentials.MicroserviceCredentials;
import com.cumulocity.microservice.context.inject.TenantScope;
import com.cumulocity.microservice.context.scope.BaseScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.CustomScopeConfigurer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static com.cumulocity.microservice.context.scope.BaseScope.DEFAULT_CACHE_EXPIRATION_TIMEOUT;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class EnableContextSupportConfigurationTest {

    public static class TenantService {
        private static int static_counter = 0;

        private final String tenant;
        private final int counter;

        public TenantService(String tenant) {
            this.tenant = tenant;
            this.counter = TenantService.static_counter++;
        }

        public String getDescription() {
            return tenant + ", " + counter;
        }
    }

    @BeforeEach
    void setUp() {
        TenantService.static_counter = 0;
    }

    @Configuration
    @EnableContextSupport
    public static class TestConfiguration {

        @Autowired
        private ContextService<MicroserviceCredentials> microservice;

        @Bean
        @TenantScope
        public TenantService getContext() {
            return new TenantService(microservice.getContext().getTenant());
        }
    }

    @Autowired
    private ContextService<MicroserviceCredentials> microservice;

    @Autowired
    private TenantService tenantService1;

    @Autowired
    private TenantService tenantService2;

    @Test
    public void shouldCreateTenantServiceOnlyOnceForTheSameTenant() {
        //given
        final MicroserviceCredentials credentials1 = new MicroserviceCredentials().withTenant("tenant");
        final MicroserviceCredentials credentials2 = new MicroserviceCredentials().withTenant("tenant");

        //when
        final String context1 = microservice.callWithinContext(credentials1, tenantService1::getDescription);
        final String context2 = microservice.callWithinContext(credentials2, tenantService2::getDescription);

        //then
        assertThat(context1).isEqualTo(context2);
    }

    @Test
    public void shouldCreateDifferentServicesForDifferentTenant() {
        //given
        final MicroserviceCredentials credentials1 = new MicroserviceCredentials().withTenant("tenant1");
        final MicroserviceCredentials credentials2 = new MicroserviceCredentials().withTenant("tenant2");

        //when
        final String context1 = microservice.callWithinContext(credentials1, tenantService1::getDescription);
        final String context2 = microservice.callWithinContext(credentials2, tenantService1::getDescription);

        //then
        assertThat(context1).isEqualTo("tenant1, 0");
        assertThat(context2).isEqualTo("tenant2, 1");
    }

    @Test
    public void shouldCreateTenantServiceOnlyOnceForTheSameTenantAndAppKey() {
        //given
        final MicroserviceCredentials credentials1 = new MicroserviceCredentials().withTenant("tenant").withAppKey("appKey");
        final MicroserviceCredentials credentials2 = new MicroserviceCredentials().withTenant("tenant").withAppKey("appKey");

        //when
        final String context1 = microservice.callWithinContext(credentials1, tenantService1::getDescription);
        final String context2 = microservice.callWithinContext(credentials2, tenantService2::getDescription);

        //then
        assertThat(context1).isEqualTo(context2);
    }

    @Test
    public void shouldCreateDifferentServicesForDifferentAppKey() {
        //given
        final MicroserviceCredentials credentials1 = new MicroserviceCredentials().withTenant("tenant").withAppKey("appKey1");
        final MicroserviceCredentials credentials2 = new MicroserviceCredentials().withTenant("tenant").withAppKey("appKey2");

        //when
        final String context1 = microservice.callWithinContext(credentials1, tenantService1::getDescription);
        final String context2 = microservice.callWithinContext(credentials2, tenantService1::getDescription);

        //then
        assertThat(context1).isEqualTo("tenant, 0");
        assertThat(context2).isEqualTo("tenant, 1");
    }

    @Nested
    @TestPropertySource(properties = {
            "tenantCacheExpirationTimeout=123456"
    })
    @Import(TestConfiguration.class)
    @SpringBootTest(classes = EnableContextSupportConfiguration.class )
    public class TenantCacheExpirationTimeoutFromPropertiesTest {
        @Autowired
        private CustomScopeConfigurer customScopeConfigurer;

        @Test
        public void shouldReadTimeoutValueFromProperties() {
            Map<String, Object> scopes = (Map<String, Object>) ReflectionTestUtils.getField(customScopeConfigurer, "scopes");

            assertThat(scopes.size()).isEqualTo(2);
            assertThat(((BaseScope) scopes.get("user")).getCacheExpirationTimeout()).isEqualTo(Long.valueOf(DEFAULT_CACHE_EXPIRATION_TIMEOUT));
            assertThat(((BaseScope) scopes.get("tenant")).getCacheExpirationTimeout()).isEqualTo(Long.valueOf("123456"));
        }
    }

    @Nested
    @Import(TestConfiguration.class)
    @SpringBootTest(classes = EnableContextSupportConfiguration.class )
    public class TenantCacheExpirationTimeoutMissingFromPropertiesTest {
        @Autowired
        private CustomScopeConfigurer customScopeConfigurer;

        @Test
        public void shouldSetTimeoutValueToDefaultWhenMissingInProperties() {
            Map<String, Object> scopes = (Map<String, Object>) ReflectionTestUtils.getField(customScopeConfigurer, "scopes");

            assertThat(scopes.size()).isEqualTo(2);
            assertThat(((BaseScope) scopes.get("user")).getCacheExpirationTimeout()).isEqualTo(Long.valueOf(DEFAULT_CACHE_EXPIRATION_TIMEOUT));
            assertThat(((BaseScope) scopes.get("tenant")).getCacheExpirationTimeout()).isEqualTo(Long.valueOf(DEFAULT_CACHE_EXPIRATION_TIMEOUT));
        }
    }
}
