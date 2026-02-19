package com.cumulocity.microservice.subscription.repository;

import com.cumulocity.microservice.subscription.model.core.PlatformProperties.IsolationLevel;
import com.cumulocity.microservice.subscription.repository.impl.CurrentMicroserviceRepository;
import com.cumulocity.microservice.subscription.repository.impl.LegacyMicroserviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

import static com.cumulocity.microservice.subscription.model.core.PlatformProperties.IsolationLevel.MULTI_TENANT;
import static com.cumulocity.microservice.subscription.repository.MicroserviceRepositoryBuilder.CUSTOM_AUTOREGISTRATION_ENV_NAME;
import static com.cumulocity.microservice.subscription.repository.MicroserviceRepositoryBuilder.MICROSERVICE_ISOLATION_ENV_NAME;
import static org.assertj.core.api.Assertions.assertThat;

public class MicroserviceRepositoryBuilderTest {

    private MicroserviceRepositoryBuilder builder;

    @BeforeEach
    public void setup() {
        builder = MicroserviceRepositoryBuilder.microserviceRepositoryBuilder()
                                               .password("$uperStr0ng1");
    }

    @Test
    public void shouldCreateLegacyRepositoryWhenNoIsolationDefined() {
        builder.environment(environmentWithIsolation(null))
                .username("servicebootstrap");

        MicroserviceRepository repository = builder.build();

        assertThat(repository).isInstanceOf(LegacyMicroserviceRepository.class);
    }

    @Test
    public void shouldCreateCurrentRepositoryWhenIsolationDefined() {
        builder.environment(environmentWithIsolation(MULTI_TENANT));

        MicroserviceRepository repository = builder.build();

        assertThat(repository).isInstanceOf(CurrentMicroserviceRepository.class);
    }

    @ParameterizedTest
    @CsvSource({
            "true,  servicebootstrap_my-application, AutoregisterMicroserviceRepository",
            "true,  servicebootstrap,                LegacyMicroserviceRepository",
            "false, servicebootstrap_my-application, LegacyMicroserviceRepository",
            "false, servicebootstrap,                LegacyMicroserviceRepository"
    })
    public void shouldCreateAutoregisterRepository_whenConfiguredViaEnvironment(boolean autoregister, String username, String expectedRepository) {
        builder.environment(environmentWithProperty(CUSTOM_AUTOREGISTRATION_ENV_NAME, autoregister))
                .username(username);

        MicroserviceRepository repository = builder.build();

        assertThat(repository.getClass().getName()).contains(expectedRepository);
    }

    @ParameterizedTest
    @CsvSource({
            "true,  servicebootstrap_my-application, AutoregisterMicroserviceRepository",
            "true,  servicebootstrap,                LegacyMicroserviceRepository",
            "false, servicebootstrap_my-application, LegacyMicroserviceRepository",
            "false, servicebootstrap,                LegacyMicroserviceRepository"
    })
    public void shouldCreateAutoregisterRepository_whenConfiguredViaCode(boolean autoregister, String username, String expectedRepository) {
        builder.autoregistation(autoregister)
               .username(username);

        MicroserviceRepository repository = builder.build();

        assertThat(repository.getClass().getName()).contains(expectedRepository);
    }

    @Test
    // This scenario is needed in e2e application behavior on real microservices. It may be caused by hidden bug somewhere,
    // but whey we tried to throw Exception, then some of the existing microservices did not start correctly
    public void shouldFallbackToLegacyRepository_whenNoData() {
        builder.environment(environmentWithIsolation(null))
                .username(null);

        MicroserviceRepository repository = builder.build();

        assertThat(repository).isInstanceOf(LegacyMicroserviceRepository.class);
    }

    private Environment environmentWithIsolation(IsolationLevel isolation) {
        MockEnvironment environment = new MockEnvironment();
        if (isolation != null) {
            environment.setProperty(MICROSERVICE_ISOLATION_ENV_NAME, isolation.toString());
        }
        return environment;
    }

    private Environment environmentWithProperty(String propertyName, Object propertyValue) {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty(propertyName, propertyValue);
        return environment;
    }

}
