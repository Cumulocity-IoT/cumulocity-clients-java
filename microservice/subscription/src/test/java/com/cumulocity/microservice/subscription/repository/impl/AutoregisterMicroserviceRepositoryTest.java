package com.cumulocity.microservice.subscription.repository.impl;

import com.cumulocity.microservice.subscription.model.MicroserviceMetadataRepresentation;
import com.cumulocity.microservice.subscription.repository.MicroserviceRepositoryBuilder;
import com.cumulocity.rest.representation.application.ApplicationRepresentation;
import com.cumulocity.rest.representation.application.ApplicationUserRepresentation;
import com.cumulocity.sdk.client.SDKException;
import com.google.common.base.Suppliers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static com.cumulocity.microservice.subscription.model.MicroserviceMetadataRepresentation.microserviceMetadataRepresentation;
import static com.cumulocity.microservice.subscription.repository.MicroserviceRepositoryBuilder.APP_SERVICEBOOTSTRAP_PREFIX;
import static com.cumulocity.microservice.subscription.repository.MicroserviceRepositoryBuilder.microserviceRepositoryBuilder;
import static com.cumulocity.microservice.subscription.repository.impl.FakeCredentialsSwitchingPlatform.asCredentials;
import static com.cumulocity.microservice.subscription.repository.impl.FakeCredentialsSwitchingPlatform.byMethod;
import static com.cumulocity.rest.representation.application.ApplicationRepresentation.MICROSERVICE;
import static com.cumulocity.rest.representation.application.ApplicationRepresentation.applicationRepresentation;
import static java.util.stream.StreamSupport.stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.springframework.http.HttpMethod.POST;

public class AutoregisterMicroserviceRepositoryTest {

    static final String BASE_URL = "http://c8y.com";
    static final String APPLICATION_NAME = "current-application-name";
    static final String TENANT = "t1000";
    static final String OLD_REQUIRED_ROLE = "ROLE_ALARM_READ";
    static final String NEW_REQUIRED_ROLE = "ROLE_ALARM_ADMIN";
    static final String LWM2M_ROLE = "ROLE_LWM2M";

    FakeCredentialsSwitchingPlatform platform = new FakeCredentialsSwitchingPlatform();

    AutoregisterMicroserviceRepository repository;

    @BeforeEach
    void setup() {
        repository = givenRepository(APPLICATION_NAME);
    }

    @Nested
    class SpecificToAutoregisterRepository {

        @Test
        void shouldUpdateApplication_whenMetadataChanges() {
            //given
            String microserviceUrl = BASE_URL + "/service/lwm2m";
            ApplicationRepresentation existing = applicationRepresentation()
                    .type(MICROSERVICE)
                    .name(APPLICATION_NAME)
                    .requiredRoles(List.of(OLD_REQUIRED_ROLE))
                    .url(microserviceUrl)
                    .roles(List.of(LWM2M_ROLE))
                    .build();
            platform.addApplication(existing);
            platform.switchTo(asCredentials(platform.bootstrapUserFor(existing)));

            //when
            MicroserviceMetadataRepresentation metadata = microserviceMetadataRepresentation()
                    .requiredRole(NEW_REQUIRED_ROLE)
                    .url(microserviceUrl)
                    .build();
            ApplicationRepresentation registered = repository.register(APPLICATION_NAME, metadata);

            //then
            assertThat(registered.getRequiredRoles()).containsExactly(NEW_REQUIRED_ROLE);   // change
            assertThat(registered.getRoles()).isEmpty();                                            // clear
            assertThat(registered.getUrl()).isEqualTo(microserviceUrl);                             // preserve
            assertThat(registered.getType()).isEqualTo(MICROSERVICE);
            assertThat(registered.getName()).isEqualTo(APPLICATION_NAME);

        }

        @Test
        void shouldNotUpdateApplication_whenImportantMetadataDoesNotChange() {
            //given
            ApplicationRepresentation existing = applicationRepresentation()
                    .type(MICROSERVICE)
                    .name(APPLICATION_NAME)
                    .requiredRoles(List.of(OLD_REQUIRED_ROLE))
                    .build();
            platform.addApplication(existing);
            platform.switchTo(asCredentials(platform.bootstrapUserFor(existing)));

            //when
            MicroserviceMetadataRepresentation metadata = microserviceMetadataRepresentation()
                    .requiredRole(OLD_REQUIRED_ROLE)
                    .build();
            ApplicationRepresentation registered = repository.register(APPLICATION_NAME, metadata);

            //then
            assertThat(registered.getRequiredRoles()).containsExactly(OLD_REQUIRED_ROLE);
        }

    }

    @Nested
    class RepeatedFromCurrentRepository {

        @Test
        public void shouldFail_whenNoCurrentApplicationForDeprecatedRegisterMethod() {
            //given
            ApplicationRepresentation notRegistered = applicationRepresentation()
                    .type(MICROSERVICE)
                    .name("lwm2m")
                    .build();
            platform.switchTo(asCredentials(platform.bootstrapUserFor(notRegistered)));

            //when
            Throwable exception = catchThrowable(() ->
                    repository.register("lwm2m", microserviceMetadataRepresentation().build()));

            //then
            assertThat(exception)
                    .isInstanceOf(SDKException.class)
                    .hasMessageContaining("Failed to load current microservice.");
        }

        @Test
        public void shouldFail_whenNoCurrentApplicationForOneArgumentRegisterMethod() {
            //given
            ApplicationRepresentation notRegistered = applicationRepresentation()
                    .type(MICROSERVICE)
                    .name("lwm2m")
                    .build();
            platform.switchTo(asCredentials(platform.bootstrapUserFor(notRegistered)));

            //when
            Throwable exception = catchThrowable(() ->
                    repository.register(microserviceMetadataRepresentation().build()));

            //then
            assertThat(exception)
                    .isInstanceOf(SDKException.class)
                    .hasMessageContaining("Failed to load current microservice.");
        }

        @Test
        void shouldNotFail_onMultipleCallsForRegisterMethod() {
            ApplicationRepresentation existing = applicationRepresentation()
                    .type(MICROSERVICE)
                    .name("existingApp")
                    .build();
            platform.addApplication(existing);
            platform.switchTo(asCredentials(platform.bootstrapUserFor(existing)));

            for (int i = 0; i < 10; ++i) {
                assertThat(repository.register("existingApp", microserviceMetadataRepresentation().build())).isNotNull();
                assertThat(platform.take(byMethod(POST))).isEmpty();
            }
        }

        @Test
        public void shouldNotFail_onMultipleCallsForRegisterMethod_oneArgument() {
            ApplicationRepresentation existing = applicationRepresentation()
                    .type(MICROSERVICE)
                    .name(APPLICATION_NAME)
                    .build();
            platform.addApplication(existing);
            platform.switchTo(asCredentials(platform.bootstrapUserFor(existing)));

            for (int i = 0; i < 10; ++i) {
                assertThat(repository.register(microserviceMetadataRepresentation().build())).isNotNull();
                assertThat(platform.take(byMethod(POST))).isEmpty();
            }
        }

        @Test
        void shouldLoadCurrentApplication() {
            //given
            ApplicationRepresentation existing = applicationRepresentation()
                    .type(MICROSERVICE)
                    .name(APPLICATION_NAME)
                    .build();
            platform.switchTo(asCredentials(platform.bootstrapUserFor(existing)));
            platform.addApplication(existing);

            //when
            ApplicationRepresentation currentApplication = repository.getCurrentApplication();

            //then
            assertThat(currentApplication).isSameAs(existing);
        }

        @Test
        public void shouldGetSubscriptions() {
            //given
            ApplicationRepresentation existing = applicationRepresentation()
                    .type(MICROSERVICE)
                    .name(APPLICATION_NAME)
                    .build();
            platform.switchTo(asCredentials(platform.bootstrapUserFor(existing)));
            ApplicationUserRepresentation applicationUserRepresentation = new ApplicationUserRepresentation();
            platform.addApplicationUserRepresentation(applicationUserRepresentation);

            //when
            Iterable<ApplicationUserRepresentation> subscriptions = repository.getSubscriptions();

            //then
            assertThat(subscriptions).hasSize(1);
            ApplicationUserRepresentation firstSubscription = stream(subscriptions.spliterator(), false).findFirst().orElseThrow();
            assertThat(firstSubscription).isSameAs(applicationUserRepresentation);
        }

        @Test
        void shouldGetSubscriptionsByNameWhichIsIgnored() {
            //given
            ApplicationRepresentation existing = applicationRepresentation()
                    .type(MICROSERVICE)
                    .name(APPLICATION_NAME)
                    .build();
            platform.switchTo(asCredentials(platform.bootstrapUserFor(existing)));
            ApplicationUserRepresentation applicationUserRepresentation = new ApplicationUserRepresentation();
            platform.addApplicationUserRepresentation(applicationUserRepresentation);

            //when
            Iterable<ApplicationUserRepresentation> subscriptions = repository.getSubscriptions("not-used-application-id");

            //then
            assertThat(subscriptions).hasSize(1);
            ApplicationUserRepresentation firstSubscription = stream(subscriptions.spliterator(), false).findFirst().orElseThrow();
            assertThat(firstSubscription).isSameAs(applicationUserRepresentation);
        }
    }

    private AutoregisterMicroserviceRepository givenRepository(String applicationName) {
        MicroserviceRepositoryBuilder builder = microserviceRepositoryBuilder()
                .autoregistation(true)
                .baseUrl(Suppliers.ofInstance(BASE_URL))
                .connector(platform)
                .environment(new MockEnvironment())
                .tenant(TENANT)
                .username(APP_SERVICEBOOTSTRAP_PREFIX + applicationName)
                .password("test")
                .applicationName(applicationName)
                .applicationKey(applicationName + "-key");
        return (AutoregisterMicroserviceRepository) builder.build();
    }

}