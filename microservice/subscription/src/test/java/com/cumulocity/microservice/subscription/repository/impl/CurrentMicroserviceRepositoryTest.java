package com.cumulocity.microservice.subscription.repository.impl;

import com.cumulocity.microservice.subscription.repository.MicroserviceRepository;
import com.cumulocity.microservice.subscription.repository.MicroserviceRepositoryBuilder;
import com.cumulocity.rest.representation.application.ApplicationRepresentation;
import com.cumulocity.rest.representation.application.ApplicationUserRepresentation;
import com.cumulocity.sdk.client.SDKException;
import com.google.common.base.Suppliers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;

import static com.cumulocity.microservice.subscription.model.MicroserviceMetadataRepresentation.microserviceMetadataRepresentation;
import static com.cumulocity.microservice.subscription.model.core.PlatformProperties.IsolationLevel.MULTI_TENANT;
import static com.cumulocity.microservice.subscription.repository.MicroserviceRepositoryBuilder.MICROSERVICE_ISOLATION_ENV_NAME;
import static com.cumulocity.microservice.subscription.repository.MicroserviceRepositoryBuilder.microserviceRepositoryBuilder;
import static com.cumulocity.microservice.subscription.repository.impl.FakeCredentialsSwitchingPlatform.asCredentials;
import static com.cumulocity.microservice.subscription.repository.impl.FakeCredentialsSwitchingPlatform.byMethod;
import static com.cumulocity.rest.representation.application.ApplicationRepresentation.MICROSERVICE;
import static com.cumulocity.rest.representation.application.ApplicationRepresentation.applicationRepresentation;
import static java.util.stream.StreamSupport.stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.springframework.http.HttpMethod.POST;

@ExtendWith(MockitoExtension.class)
public class CurrentMicroserviceRepositoryTest {

    private static final String BASE_URL = "http://c8y.com";
    private static final String CURRENT_APPLICATION_NAME = "current-application-name";

    private FakeCredentialsSwitchingPlatform platform = new FakeCredentialsSwitchingPlatform();

    private CurrentMicroserviceRepository repository;

    @BeforeEach
    public void setup() {
        MockEnvironment environment = mockEnvironment();

        MicroserviceRepositoryBuilder builder = microserviceRepositoryBuilder()
                .baseUrl(Suppliers.ofInstance(BASE_URL))
                .connector(platform)
                .environment(environment)
                .applicationName(CURRENT_APPLICATION_NAME);
        repository = (CurrentMicroserviceRepository) builder.build();
    }

    @Test
    public void shouldBuildCurrentApplicationRepositoryWithoutApplicationName(){
        //given
        MockEnvironment environment = mockEnvironment();

        //when
        MicroserviceRepository repository = microserviceRepositoryBuilder()
                .baseUrl(Suppliers.ofInstance(BASE_URL))
                .connector(platform)
                .environment(environment)
                .build();

        //then
        assertThat(repository).isNotNull();
    }

    @Test
    public void shouldFailWhenNoCurrentApplicationForDeprecatedRegisterMethod() {
        ApplicationRepresentation notRegistered = applicationRepresentation()
                .type(MICROSERVICE)
                .name("cep")
                .build();
        platform.switchTo(asCredentials(platform.bootstrapUserFor(notRegistered)));

        Throwable exception = catchThrowable(() ->
                repository.register("cep", microserviceMetadataRepresentation().build()));

        assertThat(exception)
                .isInstanceOf(SDKException.class)
                .hasMessageContaining("Failed to load current microservice.");
    }

    @Test
    public void shouldFailWhenNoCurrentApplicationForOneArgumentRegisterMethod() {
        ApplicationRepresentation notRegistered = applicationRepresentation()
                .type(MICROSERVICE)
                .name("cep")
                .build();
        platform.switchTo(asCredentials(platform.bootstrapUserFor(notRegistered)));

        Throwable exception = catchThrowable(() ->
                repository.register(microserviceMetadataRepresentation().build()));

        assertThat(exception)
                .isInstanceOf(SDKException.class)
                .hasMessageContaining("Failed to load current microservice.");
    }

    @Test
    public void shouldNotFailOnMultipleCallsForDeprecatedRegisterMethod() {
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
    public void shouldNotFailWhenDeprecatedRegisterMethodIsInvokedAndRepositoryWasCreatedWithoutCurrentApplicationName() {
        //given
        ApplicationRepresentation existing = applicationRepresentation()
                .type(MICROSERVICE)
                .name("existingApp")
                .build();
        platform.addApplication(existing);
        platform.switchTo(asCredentials(platform.bootstrapUserFor(existing)));
        MicroserviceRepository repository = repositoryWithoutCurrentApplicationName();

        //when
        ApplicationRepresentation application = repository.register("existingApp", microserviceMetadataRepresentation().build());

        //then
        assertThat(application).isNotNull();
        assertThat(platform.take(byMethod(POST))).isEmpty();
    }

    @Test
    public void shouldNotFailOnMultipleCallsForForOneArgumentRegisterMethod() {
        ApplicationRepresentation existing = applicationRepresentation()
                .type(MICROSERVICE)
                .name(CURRENT_APPLICATION_NAME)
                .build();
        platform.addApplication(existing);
        platform.switchTo(asCredentials(platform.bootstrapUserFor(existing)));

        for (int i = 0; i < 10; ++i) {
            assertThat(repository.register(microserviceMetadataRepresentation().build())).isNotNull();
            assertThat(platform.take(byMethod(POST))).isEmpty();
        }
    }

    @Test
    public void shouldLoadCurrentApplication(){
        //given
        ApplicationRepresentation existing = applicationRepresentation()
                .type(MICROSERVICE)
                .name(CURRENT_APPLICATION_NAME)
                .build();
        platform.switchTo(asCredentials(platform.bootstrapUserFor(existing)));
        platform.addApplication(existing);

        //when
        ApplicationRepresentation application = repository.getCurrentApplication();

        //then
        assertThat(application).isSameAs(existing);
    }

    @Test
    public void shouldGetSubscriptions(){
        //given
        ApplicationRepresentation existing = applicationRepresentation()
                .type(MICROSERVICE)
                .name(CURRENT_APPLICATION_NAME)
                .build();
        platform.switchTo(asCredentials(platform.bootstrapUserFor(existing)));
        ApplicationUserRepresentation applicationUserRepresentation = new ApplicationUserRepresentation();
        platform.addApplicationUserRepresentation(applicationUserRepresentation);


        //when
        Iterable<ApplicationUserRepresentation> subscriptions = repository.getSubscriptions();

        //when
        assertThat(subscriptions).hasSize(1);
        ApplicationUserRepresentation firstSubscription = stream(subscriptions.spliterator(), false).findFirst().get();
        assertThat(firstSubscription).isSameAs(applicationUserRepresentation);
    }

    @Test
    public void shouldGetSubscriptionsByNameWhichIsIgnored(){
        //given
        ApplicationRepresentation existing = applicationRepresentation()
                .type(MICROSERVICE)
                .name(CURRENT_APPLICATION_NAME)
                .build();
        platform.switchTo(asCredentials(platform.bootstrapUserFor(existing)));
        ApplicationUserRepresentation applicationUserRepresentation = new ApplicationUserRepresentation();
        platform.addApplicationUserRepresentation(applicationUserRepresentation);


        //when
        Iterable<ApplicationUserRepresentation> subscriptions = repository.getSubscriptions("not-used-application-id");

        //when
        assertThat(subscriptions).hasSize(1);
        ApplicationUserRepresentation firstSubscription = stream(subscriptions.spliterator(), false).findFirst().get();
        assertThat(firstSubscription).isSameAs(applicationUserRepresentation);
    }



    private MockEnvironment mockEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty(MICROSERVICE_ISOLATION_ENV_NAME, MULTI_TENANT.toString());
        return environment;
    }

    private MicroserviceRepository repositoryWithoutCurrentApplicationName() {
        MockEnvironment environment = mockEnvironment();
        return microserviceRepositoryBuilder()
                .baseUrl(Suppliers.ofInstance(BASE_URL))
                .connector(platform)
                .environment(environment)
                .build();
    }

}
