package com.cumulocity.microservice.subscription.repository.impl;

import com.cumulocity.microservice.subscription.model.MicroserviceMetadataRepresentation;
import com.cumulocity.microservice.subscription.repository.MicroserviceRepository;
import com.cumulocity.microservice.subscription.repository.MicroserviceRepositoryBuilder;
import com.cumulocity.rest.representation.application.ApplicationCollectionRepresentation;
import com.cumulocity.rest.representation.application.ApplicationRepresentation;
import com.cumulocity.rest.representation.application.ApplicationUserRepresentation;
import com.cumulocity.rest.representation.application.microservice.ExtensionRepresentation;
import com.google.common.base.Predicate;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;
import org.springframework.mock.env.MockEnvironment;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.StreamSupport;

import static com.cumulocity.microservice.subscription.model.MicroserviceMetadataRepresentation.microserviceMetadataRepresentation;
import static com.cumulocity.microservice.subscription.repository.MicroserviceRepositoryBuilder.GLOBAL_SERVICEBOOTSTRAP_USER;
import static com.cumulocity.microservice.subscription.repository.MicroserviceRepositoryBuilder.microserviceRepositoryBuilder;
import static com.cumulocity.rest.representation.application.ApplicationRepresentation.MICROSERVICE;
import static com.cumulocity.rest.representation.application.ApplicationRepresentation.applicationRepresentation;
import static com.cumulocity.rest.representation.application.ApplicationUserRepresentation.applicationUserRepresentation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

public class LegacyMicroserviceRepositoryTest {

    private static final String BASE_URL = "http://c8y.com";
    private static final String CURRENT_APPLICATION_NAME = "current-application-name";
    private static final String CURRENT_APPLICATION_KEY = "current-application-key";

    private final FakeCredentialsSwitchingPlatform platform = new FakeCredentialsSwitchingPlatform();

    private LegacyMicroserviceRepository repository;

    @BeforeEach
    public void setup() {
        repository = givenRepository(CURRENT_APPLICATION_NAME, CURRENT_APPLICATION_KEY);
    }

    @Test
    public void shouldNotRequireCurrentApplicationName(){
        //when
        MicroserviceRepository repository = givenRepository(null, null);

        //then
        assertThat(repository).isNotNull();
    }

    @Test
    public void shouldNotRegisterApplicationWhenApplicationNameWasNotProvidedToConstructor(){
        //given
        final MicroserviceRepository repository = givenRepository(null, null);

        //when
        Throwable throwable = catchThrowable(() -> repository.register(microserviceMetadataRepresentation().build()));

        //then
        assertThat(throwable)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Application name must be provided at construction time");
    }

    @Test
    public void shouldNotGetCurrentApplicationWhenApplicationNameWasNotProvidedToConstructor(){
        //given
        final MicroserviceRepository repository = givenRepository(null, null);

        //when
        Throwable throwable = catchThrowable(() -> repository.getCurrentApplication());

        //then
        assertThat(throwable)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("You need to provide current application name at construction");
    }

    @Test
    public void shouldGetCurrentApplication(){
        //given
        ApplicationRepresentation existing = applicationRepresentation()
                .id("3")
                .type(MICROSERVICE)
                .name(CURRENT_APPLICATION_NAME)
                .key(CURRENT_APPLICATION_KEY)
                .build();
        givenApplications(existing);

        //when
        ApplicationRepresentation currentApplication = repository.getCurrentApplication();

        //then
        assertThat(currentApplication)
                .isNotNull()
                .isSameAs(existing);
    }

    @Test
    public void shouldUpdateApplicationWhenAlreadyExistsAndRegistrationIsEnabledWhenDeprecatedRegisterMethodIsUsed() {

        ApplicationRepresentation existing = applicationRepresentation()
                .id("3")
                .type(MICROSERVICE)
                .name("cep")
                .key("cep-key")
                .build();
        givenApplications(existing);

        ApplicationRepresentation cep = repository.register("cep", microserviceMetadataRepresentation()
                .role("TEST").requiredRole("TEST").build());

        assertThat(cep).isEqualTo(existing);

        assertThat(platform.take(byMethod(PUT)))
                .hasSize(1)
                .extracting("body")
                .have(new Condition<Object>() {
                    @Override
                    public boolean matches(Object value) {
                        if (value instanceof ApplicationRepresentation) {
                            assertThat(((ApplicationRepresentation) value).getRequiredRoles()).contains("TEST");
                            assertThat(((ApplicationRepresentation) value).getRoles()).contains("TEST");
                            return true;
                        }
                        return false;
                    }
                });
        assertThat(platform.take(byMethod(POST))).isEmpty();
    }

    @Test
    public void shouldUpdateApplicationWhenAlreadyExistsAndRegistrationIsEnabledWhenOneArgumentRegisterMethodIsUsed() {

        ApplicationRepresentation existing = applicationRepresentation()
                .id("3")
                .type(MICROSERVICE)
                .name(CURRENT_APPLICATION_NAME)
                .key(CURRENT_APPLICATION_KEY)
                .build();
        givenApplications(existing);

        ApplicationRepresentation cep = repository.register(microserviceMetadataRepresentation()
                .role("TEST").requiredRole("TEST").build());

        assertThat(cep).isEqualTo(existing);

        assertThat(platform.take(byMethod(PUT)))
                .hasSize(1)
                .extracting("body")
                .have(new Condition<Object>() {
                    @Override
                    public boolean matches(Object value) {
                        if (value instanceof ApplicationRepresentation) {
                            assertThat(((ApplicationRepresentation) value).getRequiredRoles()).contains("TEST");
                            assertThat(((ApplicationRepresentation) value).getRoles()).contains("TEST");
                            return true;
                        }
                        return false;
                    }
                });
        assertThat(platform.take(byMethod(POST))).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "'notExistingApp', , 'notExistingApp-application-key'",
            "'notExistingApp', '', 'notExistingApp-application-key'",
            "'notExistingApp', 'notExistingApp-key', 'notExistingApp-key'",
    })
    public void shouldRegisterApplicationWhenThereIsNoExistingWhenOneArgumentRegisterMethodIsUsed(String appName, String configuredAppKey, String resultAppKey) {
        givenApplications();
        MicroserviceRepository repository = givenRepository(appName, configuredAppKey);

        repository.register(microserviceMetadataRepresentation().build());

        Collection<FakeCredentialsSwitchingPlatform.Request> posts = platform.take(byMethod(POST));
        assertThat(posts).isNotEmpty();
        assertThat(posts)
                .hasSize(1)
                .extracting("body")
                .containsExactly(applicationRepresentation()
                        .id("0")
                        .name(appName)
                        .type("MICROSERVICE")
                        .key(resultAppKey)
                        .requiredRoles(ImmutableList.<String>of())
                        .roles(ImmutableList.<String>of())
                        .build());
    }

    @ParameterizedTest
    @CsvSource({
            "'notExistingApp', , 'notExistingApp-application-key'",
            "'notExistingApp', '', 'notExistingApp-application-key'",
            "'notExistingApp', 'notExistingApp-key', 'notExistingApp-key'",
    })
    public void shouldRegisterApplicationWhenThereIsNoExistingWhenDeprecatedRegisterMethodIsUsed(String appName, String configuredAppKey, String resultAppKey) {
        givenApplications();
        MicroserviceRepository repository = givenRepository(CURRENT_APPLICATION_NAME, configuredAppKey);

        repository.register(appName, microserviceMetadataRepresentation().build());

        Collection<FakeCredentialsSwitchingPlatform.Request> posts = platform.take(byMethod(POST));
        assertThat(posts).isNotEmpty();
        assertThat(posts)
                .hasSize(1)
                .extracting("body")
                .containsExactly(applicationRepresentation()
                        .id("0")
                        .name(appName)
                        .type("MICROSERVICE")
                        .key(resultAppKey)
                        .requiredRoles(ImmutableList.<String>of())
                        .roles(ImmutableList.<String>of())
                        .build());
    }

    @ParameterizedTest
    @CsvSource({
            "'notExistingApp', , 'notExistingApp-application-key'",
            "'notExistingApp', '', 'notExistingApp-application-key'",
            "'notExistingApp', 'notExistingApp-key', 'notExistingApp-key'",
    })
    public void shouldRegisterApplicationWithDeprecatedMethodWhenApplicationNameWasNotPassedToConstructor(String appName, String configuredAppKey, String resultAppKey) {
        //given
        givenApplications();
        MicroserviceRepository repository = givenRepository(null, configuredAppKey);

        repository.register(appName, microserviceMetadataRepresentation().build());

        Collection<FakeCredentialsSwitchingPlatform.Request> posts = platform.take(byMethod(POST));
        assertThat(posts).isNotEmpty();
        assertThat(posts)
                .hasSize(1)
                .extracting("body")
                .containsExactly(applicationRepresentation()
                        .id("0")
                        .name(appName)
                        .type("MICROSERVICE")
                        .key(resultAppKey)
                        .requiredRoles(ImmutableList.<String>of())
                        .roles(ImmutableList.<String>of())
                        .build());
    }

    @Test
    public void shouldNotRegisterApplicationWhenThereIsExistingWhenDeprecatedRegisterMethodIsUsed() {
        givenApplications(applicationRepresentation().id("5").name("existingApp").type("MICROSERVICE").key("existingApp-application-key").build());

        repository.register("existingApp", microserviceMetadataRepresentation().build());

        assertThat(platform.take(byMethod(POST))).isEmpty();
    }

    @Test
    public void shouldNotRegisterApplicationWhenThereIsExistingWhenOneArgumentRegisterMethodIsUsed() {
        givenApplications(applicationRepresentation()
                .id("5")
                .name(CURRENT_APPLICATION_NAME)
                .type("MICROSERVICE")
                .key(String.format("%s-application-key", CURRENT_APPLICATION_NAME))
                .build());

        repository.register(microserviceMetadataRepresentation().build());

        assertThat(platform.take(byMethod(POST))).isEmpty();
    }

    @Test
    public void shouldNotFailOnMultipleCallsWhenOneArgumentRegisterMethodIsUsed() {
        assertThat(repository.register(microserviceMetadataRepresentation().build())).isNotNull();
        assertThat(platform.take(byMethod(POST))).isNotEmpty();

        for (int i = 0; i < 10; ++i) {
            assertThat(repository.register(microserviceMetadataRepresentation().build())).isNotNull();
            assertThat(platform.take(byMethod(POST))).isEmpty();
        }
    }

    @Test
    public void shouldNotFailOnMultipleCallsWhenDeprecatedRegisterMethodIsUsed() {
        assertThat(repository.register("existingApp", microserviceMetadataRepresentation().build())).isNotNull();
        assertThat(platform.take(byMethod(POST))).isNotEmpty();

        for (int i = 0; i < 10; ++i) {
            assertThat(repository.register("existingApp", microserviceMetadataRepresentation().build())).isNotNull();
            assertThat(platform.take(byMethod(POST))).isEmpty();
        }
    }

    @Test
    public void shouldGetSubscriptions(){
        //given
        ApplicationRepresentation existing = applicationRepresentation()
                .id("3")
                .type(MICROSERVICE)
                .name(CURRENT_APPLICATION_NAME)
                .build();
        givenApplications(existing);
        ApplicationUserRepresentation user = givenApplicationUser();

        //when
        Iterable<ApplicationUserRepresentation> subscriptions = repository.getSubscriptions();

        //then
        assertThat(subscriptions)
                .isNotNull()
                .hasSize(1);
        ApplicationUserRepresentation firstSubscription = StreamSupport.stream(subscriptions.spliterator(), false).findFirst()
                .get();
        assertThat(firstSubscription)
                .isNotNull()
                .isSameAs(user);
    }

    @Test
    public void shouldCreateAppWithExtensionsIfMetadataContains() {
        MicroserviceMetadataRepresentation metadata = givenMicroserviceMetadataWithExtensions();

        repository.register(metadata);

        assertThat(platform.take(byMethod(POST)))
                .hasSize(1)
                .extracting("body")
                .have(appRepresentationConditionWithExtensions());
        assertThat(platform.take(byMethod(PUT))).isEmpty();
    }

    @Test
    public void shouldUpdateAppWithExtensionsIfMetadataContains() {
        ApplicationRepresentation existing = applicationRepresentation()
                .id("3")
                .type(MICROSERVICE)
                .name(CURRENT_APPLICATION_NAME)
                .build();
        givenApplications(existing);
        MicroserviceMetadataRepresentation metadata = givenMicroserviceMetadataWithExtensions();

        repository.register(metadata);

        assertThat(platform.take(byMethod(PUT)))
                .hasSize(1)
                .extracting("body")
                .have(appRepresentationConditionWithExtensions());
        assertThat(platform.take(byMethod(POST))).isEmpty();
    }

    @Test
    public void shouldCreateAppWithoutExtensionsIfMetadataNotContains() {
        repository.register(microserviceMetadataRepresentation().build());

        assertThat(platform.take(byMethod(POST)))
                .hasSize(1)
                .extracting("body")
                .have(appRepresentationConditionWithoutExtensions());
        assertThat(platform.take(byMethod(PUT))).isEmpty();
    }

    @Test
    public void shouldUpdateAppWithoutExtensionsIfMetadataNotContains() {
        ApplicationRepresentation existing = applicationRepresentation()
                .id("3")
                .type(MICROSERVICE)
                .name(CURRENT_APPLICATION_NAME)
                .build();
        existing.set(givenMicroserviceMetadataWithExtensions().getExtensions(), MicroserviceMetadataRepresentation.EXTENSIONS_FIELD_NAME);
        givenApplications(existing);

        repository.register(microserviceMetadataRepresentation().build());

        assertThat(platform.take(byMethod(PUT)))
                .hasSize(1)
                .extracting("body")
                .have(appRepresentationConditionWithoutExtensions());
        assertThat(platform.take(byMethod(POST))).isEmpty();
    }

    private Predicate<FakeCredentialsSwitchingPlatform.Request> byMethod(final HttpMethod method) {
        return request -> request.getMethod().equals(method);
    }

    private void givenApplications(ApplicationRepresentation... applications) {

        ApplicationCollectionRepresentation collection = new ApplicationCollectionRepresentation();
        collection.setApplications(Arrays.asList(applications));
        for (ApplicationRepresentation app : applications) {
            platform.addApplication(app);
        }
    }

    private ApplicationUserRepresentation givenApplicationUser() {
        final ApplicationUserRepresentation user = applicationUserRepresentation()
                .tenant("t1000")
                .name("scott")
                .password("p4sswd")
                .build();
        platform.addApplicationUserRepresentation(user);
        return user;
    }

    private LegacyMicroserviceRepository givenRepository(String applicationName, String applicationKey) {
        MicroserviceRepositoryBuilder builder = microserviceRepositoryBuilder()
                .baseUrl(Suppliers.ofInstance(BASE_URL))
                .connector(platform)
                .environment(new MockEnvironment())
                .username(GLOBAL_SERVICEBOOTSTRAP_USER)
                .password("test")
                .applicationName(applicationName)
                .applicationKey(applicationKey);
        return (LegacyMicroserviceRepository) builder.build();
    }

    private MicroserviceMetadataRepresentation givenMicroserviceMetadataWithExtensions() {
        return microserviceMetadataRepresentation()
                .extensions(Arrays.asList(new ExtensionRepresentation()))
                .build();
    }

    private Condition<? super Object> appRepresentationConditionWithExtensions() {
        return new Condition<Object>() {
            @Override
            public boolean matches(Object value) {
                if (value instanceof ApplicationRepresentation) {
                    assertThat(((ApplicationRepresentation) value).get(MicroserviceMetadataRepresentation.EXTENSIONS_FIELD_NAME)).isNotNull();
                    return true;
                }
                return false;
            }
        };
    }

    private Condition<? super Object> appRepresentationConditionWithoutExtensions() {
        return new Condition<Object>() {
            @Override
            public boolean matches(Object value) {
                if (value instanceof ApplicationRepresentation) {
                    assertThat(((ApplicationRepresentation) value).get(MicroserviceMetadataRepresentation.EXTENSIONS_FIELD_NAME)).isNull();
                    return true;
                }
                return false;
            }
        };
    }
}
