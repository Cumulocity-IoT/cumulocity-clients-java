package com.cumulocity.microservice.subscription.repository;

import com.cumulocity.microservice.subscription.repository.application.ApplicationApiRepresentation;
import com.cumulocity.microservice.subscription.repository.impl.AutoregisterMicroserviceRepository;
import com.cumulocity.microservice.subscription.repository.impl.CurrentMicroserviceRepository;
import com.cumulocity.microservice.subscription.repository.impl.LegacyMicroserviceRepository;
import com.cumulocity.model.authentication.CumulocityBasicCredentials;
import com.cumulocity.model.authentication.CumulocityCredentials;
import com.google.common.base.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;

import static java.lang.Boolean.TRUE;

@Slf4j
public class MicroserviceRepositoryBuilder {

    public static final String MICROSERVICE_ISOLATION_ENV_NAME = "C8Y.microservice.isolation";
    public static final String CUSTOM_AUTOREGISTRATION_ENV_NAME = "C8Y.custom.autoregistration";
    public static final String GLOBAL_SERVICEBOOTSTRAP_USER = "servicebootstrap";
    public static final String APP_SERVICEBOOTSTRAP_PREFIX = "servicebootstrap_";

    private Supplier<String> baseUrl;
    private String tenant;
    private String username;
    private String password;
    private CredentialsSwitchingPlatform connector;
    private Environment environment;
    private String applicationName;
    private String applicationKey;

    // this value is useful for non Spring configurations, when there is no Environment and no parameter passed by environment
    private boolean defaultAutoregistration = false;

    /**
     * creates MicroserviceRepository implementation according to some env variables and passed user:
     * - when C8Y.microservice.isolation is defined, then microservice runs on new SDK in kubernetes (Current)
     * - when C8Y.microservice.isolation not defined, then microservice should support old SDK (Legacy)
     * - when C8Y.custom.autoregistration is set to "true", and user passed to builder is 'current servicebootstrap' (eg. servicebootstrap_lwm2m)
     *    then microservice should support helm-based deployments (Autoregistration)
     * - default value is Legacy
     *
     * @return microservice repository instance
     */
    public MicroserviceRepository build() {
        final CumulocityCredentials credentials = CumulocityBasicCredentials.builder()
                .username(username)
                .password(password)
                .tenantId(tenant)
                .build();
        CredentialsSwitchingPlatform connector = this.connector != null ? this.connector : new DefaultCredentialsSwitchingPlatform(baseUrl).switchTo(credentials);
        ApplicationApiRepresentation api = ApplicationApiRepresentation.of(baseUrl);
        Environment env = environment == null ? new StandardEnvironment() : environment;

        if (env.containsProperty(MICROSERVICE_ISOLATION_ENV_NAME)) {
            // per ms credentials, e.g. servicebootstrap_reporting-agent
            log.info("Creating CurrentMicroserviceRepository with user: {}", username);
            return new CurrentMicroserviceRepository(connector, api);
        }
        if (isAutoregistrationMode(env) && isCurrentBootstrapUser(username)) {
            // per ms credentials, internal only, e.g. servicebootstrap_lwm2m
            log.info("Creating AutoregisterMicroserviceRepository with user: {}", username);
            return new AutoregisterMicroserviceRepository(connector, api);
        }
        if (isGlobalBoostrapUser(username)) {
            // global servicebootstrap credentials (with role subscriptions_read)
            log.info("Creating LegacyMicroserviceRepository with user: {}", username);
            return new LegacyMicroserviceRepository(applicationName, applicationKey, connector, api);
        }
        log.warn("Unexpected configuration for bootstrap. User: {}, isolation: {}, autoregister: {}. Will fallback to LegacyMicroserviceRepository",
                username, env.getProperty(MICROSERVICE_ISOLATION_ENV_NAME), env.getProperty(CUSTOM_AUTOREGISTRATION_ENV_NAME));
        return new LegacyMicroserviceRepository(applicationName, applicationKey, connector, api);
    }

    private boolean isAutoregistrationMode(Environment env) {
        return defaultAutoregistration || TRUE.equals(env.getProperty(CUSTOM_AUTOREGISTRATION_ENV_NAME, Boolean.class));
    }

    private static boolean isGlobalBoostrapUser(String username) {
        return GLOBAL_SERVICEBOOTSTRAP_USER.equals(username);
    }

    private static boolean isCurrentBootstrapUser(String username) {
        return username != null && username.startsWith(APP_SERVICEBOOTSTRAP_PREFIX);
    }

    private MicroserviceRepositoryBuilder() {
    }

    public static MicroserviceRepositoryBuilder microserviceRepositoryBuilder() {
        return new MicroserviceRepositoryBuilder();
    }

    public MicroserviceRepositoryBuilder baseUrl(Supplier<String> baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public MicroserviceRepositoryBuilder tenant(String tenant) {
        this.tenant = tenant;
        return this;
    }

    public MicroserviceRepositoryBuilder username(String username) {
        this.username = username;
        return this;
    }

    public MicroserviceRepositoryBuilder applicationName(String applicationName) {
        this.applicationName = applicationName;
        return this;
    }

    public MicroserviceRepositoryBuilder applicationKey(String applicationKey) {
        this.applicationKey = applicationKey;
        return this;
    }

    public MicroserviceRepositoryBuilder password(String password) {
        this.password = password;
        return this;
    }

    public MicroserviceRepositoryBuilder connector(CredentialsSwitchingPlatform connector) {
        this.connector = connector;
        return this;
    }

    public MicroserviceRepositoryBuilder environment(Environment environment) {
        this.environment = environment;
        return this;
    }

    public MicroserviceRepositoryBuilder autoregistation(boolean defaultAutoregistration) {
        this.defaultAutoregistration = defaultAutoregistration;
        return this;
    }

}
