package com.cumulocity.microservice.subscription.annotation;

import com.cumulocity.microservice.context.credentials.Credentials;
import com.cumulocity.microservice.context.inject.TenantScope;
import com.cumulocity.microservice.properties.ConfigurationFileProvider;
import com.cumulocity.microservice.subscription.model.MicroserviceMetadataRepresentation;
import com.cumulocity.microservice.subscription.model.core.PlatformProperties;
import com.cumulocity.microservice.subscription.repository.DefaultCredentialsSwitchingPlatform;
import com.cumulocity.microservice.subscription.repository.MicroserviceRepository;
import com.cumulocity.microservice.subscription.repository.MicroserviceRepositoryBuilder;
import com.cumulocity.microservice.subscription.repository.MicroserviceSubscriptionsRepository;
import com.cumulocity.microservice.subscription.repository.application.ApplicationApi;
import com.cumulocity.microservice.subscription.service.MicroserviceSubscriptionsService;
import com.cumulocity.model.JSONBase;
import com.cumulocity.model.authentication.CumulocityBasicCredentials;
import com.cumulocity.rest.representation.application.MicroserviceManifestRepresentation;
import com.cumulocity.sdk.client.RestOperations;
import com.google.common.base.Charsets;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Configuration
@ComponentScan(basePackageClasses = {
        MicroserviceSubscriptionsService.class,
        MicroserviceSubscriptionsRepository.class,
})
@ConditionalOnProperty(value = "microservice.subscription.enabled", havingValue = "true", matchIfMissing = true)
public class EnableMicroserviceSubscriptionConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public PlatformProperties.PlatformPropertiesProvider platformPropertiesProvider() {
        return new PlatformProperties.PlatformPropertiesProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public PlatformProperties platformProperties(PlatformProperties.PlatformPropertiesProvider platformPropertiesProvider) {
        return platformPropertiesProvider.platformProperties(null);
    }

    @Bean
    @ConditionalOnMissingBean
    public MicroserviceRepository microserviceRepository(final PlatformProperties properties, final Environment environment) {
        final Credentials bootstrapUser = properties.getMicroserviceBoostrapUser();
        String applicationName = properties.getApplicationName();
        log.info("Microservice repository will be build for application '{}'.", applicationName);
        return MicroserviceRepositoryBuilder.microserviceRepositoryBuilder()
                .baseUrl(properties.getUrl())
                .environment(environment)
                .username(bootstrapUser.getUsername())
                .password(bootstrapUser.getPassword())
                .tenant(bootstrapUser.getTenant())
                .connector(new DefaultCredentialsSwitchingPlatform(properties.getUrl())
                        .switchTo(CumulocityBasicCredentials.builder()
                                .username(bootstrapUser.getUsername())
                                .password(bootstrapUser.getPassword())
                                .tenantId(bootstrapUser.getTenant())
                                .build()))
                .applicationName(applicationName)
                .applicationKey(properties.getApplicationKey())
                .build();
    }

    @Bean
    @Order
    @ConditionalOnMissingBean
    public MicroserviceMetadataRepresentation metadata(Environment environment) throws IOException {
        ConfigurationFileProvider provider = new ConfigurationFileProvider(environment);

        final Iterable<Path> manifests = provider.find(new String[]{"cumulocity"}, ".json");
        if (!Iterables.isEmpty(manifests)) {
            try (final BufferedReader reader = Files.newBufferedReader(Iterables.getFirst(manifests, null), Charsets.UTF_8)) {
                final MicroserviceManifestRepresentation manifest = JSONBase.fromJSON(reader, MicroserviceManifestRepresentation.class);
                return MicroserviceMetadataRepresentation.microserviceMetadataRepresentation()
                        .requiredRoles(MoreObjects.firstNonNull(manifest.getRequiredRoles(), ImmutableList.<String>of()))
                        .roles(MoreObjects.firstNonNull(manifest.getRoles(), ImmutableList.<String>of()))
                        .extensions(manifest.getExtensions())
                        .build();
            }
        }

        return new MicroserviceMetadataRepresentation();
    }

    @Bean
    @TenantScope
    @ConditionalOnMissingBean
    public ApplicationApi applicationApi(RestOperations restOperations) {
        return new ApplicationApi(restOperations);
    }
}
