package com.cumulocity.microservice.subscription.repository.impl;

import com.cumulocity.microservice.subscription.model.MicroserviceMetadataRepresentation;
import com.cumulocity.microservice.subscription.repository.CredentialsSwitchingPlatform;
import com.cumulocity.microservice.subscription.repository.MicroserviceRepository;
import com.cumulocity.microservice.subscription.repository.application.ApplicationApi;
import com.cumulocity.microservice.subscription.repository.application.ApplicationApiRepresentation;
import com.cumulocity.microservice.subscription.repository.application.CurrentApplicationApi;
import com.cumulocity.rest.representation.application.ApplicationRepresentation;
import com.cumulocity.rest.representation.application.ApplicationUserRepresentation;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Objects;

import static com.cumulocity.microservice.subscription.repository.impl.CurrentMicroserviceRepository.handleException;
import static lombok.AccessLevel.PRIVATE;
import static org.apache.commons.collections.CollectionUtils.isEqualCollection;

/**
 * This class should be used with services which are not managed by Core Platform.
 * This usually means internal Cumulocity services with customized helm deployments,
 * it works properly with "current bootstrap" user credentials (eg. servicebootstrap_lwm2m)
 */
@Slf4j
@Setter(value = PRIVATE)
public class AutoregisterMicroserviceRepository implements MicroserviceRepository {

    private final CredentialsSwitchingPlatform platform;
    private final ApplicationApiRepresentation api;
    private final CurrentMicroserviceRepository delegate;

    public AutoregisterMicroserviceRepository(CredentialsSwitchingPlatform platform, ApplicationApiRepresentation api) {
        this.platform = platform;
        this.api = api;
        this.delegate = new CurrentMicroserviceRepository(platform, api);
    }

    @Override
    public ApplicationRepresentation register(final MicroserviceMetadataRepresentation metadata) {
        log.info("Self registration procedure start for current application with {}", metadata);
        // load existing application checking proper state
        ApplicationRepresentation application = delegate.register(metadata);
        return update(application, metadata);
    }

    @Override
    public ApplicationRepresentation register(final String applicationName, final MicroserviceMetadataRepresentation metadata) {
        return register(metadata);
    }

    @Override
    public ApplicationRepresentation getCurrentApplication() {
        return delegate.getCurrentApplication();
    }

    @Override
    public Iterable<ApplicationUserRepresentation> getSubscriptions() {
        return delegate.getSubscriptions();
    }

    @Override
    public Iterable<ApplicationUserRepresentation> getSubscriptions(String notUsedApplicationId) {
        return delegate.getSubscriptions(notUsedApplicationId);
    }

    private ApplicationRepresentation update(ApplicationRepresentation source, MicroserviceMetadataRepresentation metadata) {
        if (noChangeInApplicationMetadata(source, metadata)) {
            log.info("Not updating current application during autoregistration. Application is up to date.");
            return source;
        }
        try {
            ApplicationRepresentation application = new ApplicationRepresentation();
            application.setId(source.getId());
            application.setRequiredRoles(metadata.getRequiredRoles());
            application.setRoles(metadata.getRoles());
            application.setUrl(metadata.getUrl());
            application.set(metadata.getExtensions(), MicroserviceMetadataRepresentation.EXTENSIONS_FIELD_NAME);
            log.info("Updating current application based on metadata");
            return currentApplicationApi().update(application);
        } catch (Exception ex) {
            log.warn("Failed to update current application. Msg: {}", ex.getMessage());
            return (ApplicationRepresentation) handleException("PUT", api.getByIdUrl(source.getId()), ex);
        }
    }

    private boolean noChangeInApplicationMetadata(ApplicationRepresentation source, MicroserviceMetadataRepresentation metadata) {
        return isEqualCollectionNullSafe(source.getRequiredRoles(), metadata.getRequiredRoles()) &&
               isEqualCollectionNullSafe(source.getRoles(), metadata.getRoles()) &&
               Objects.equals(source.getUrl(), metadata.getUrl()) &&
               extentionsEqualNullSafe(source, metadata);
    }

    private static boolean isEqualCollectionNullSafe(Collection<?> c1, Collection<?> c2) {
        return c1 == null && c2 == null ||
               c1 != null && c2 != null && isEqualCollection(c1, c2);
    }

    private CurrentApplicationApi currentApplicationApi() {
        return new ApplicationApi(platform.get(), api).currentApplication();
    }

    private static boolean extentionsEqualNullSafe(ApplicationRepresentation source, MicroserviceMetadataRepresentation metadata) {
        Object sourceExtensions = source.get(MicroserviceMetadataRepresentation.EXTENSIONS_FIELD_NAME);
        return sourceExtensions == null && metadata.getExtensions() == null
                ||
                sourceExtensions instanceof Collection<?> extensionsCollection &&
                        isEqualCollectionNullSafe(extensionsCollection, metadata.getExtensions());
    }

}
