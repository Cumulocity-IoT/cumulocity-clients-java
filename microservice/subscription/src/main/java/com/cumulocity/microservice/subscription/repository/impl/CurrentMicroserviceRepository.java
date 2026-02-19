package com.cumulocity.microservice.subscription.repository.impl;

import com.cumulocity.microservice.subscription.model.MicroserviceMetadataRepresentation;
import com.cumulocity.microservice.subscription.repository.CredentialsSwitchingPlatform;
import com.cumulocity.microservice.subscription.repository.MicroserviceRepository;
import com.cumulocity.microservice.subscription.repository.application.ApplicationApi;
import com.cumulocity.microservice.subscription.repository.application.ApplicationApiRepresentation;
import com.cumulocity.rest.representation.application.ApplicationRepresentation;
import com.cumulocity.rest.representation.application.ApplicationUserCollectionRepresentation;
import com.cumulocity.rest.representation.application.ApplicationUserRepresentation;
import com.cumulocity.sdk.client.RestOperations;
import com.cumulocity.sdk.client.SDKException;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


import static com.cumulocity.rest.representation.application.ApplicationMediaType.APPLICATION_USER_COLLECTION_MEDIA_TYPE;
import static com.cumulocity.rest.representation.application.ApplicationRepresentation.MICROSERVICE;
import static lombok.AccessLevel.PRIVATE;
import static org.apache.http.HttpStatus.*;

/**
 * works with platform API &gt;= 8.18
 */
@Slf4j
@Setter(value = PRIVATE)
public class CurrentMicroserviceRepository implements MicroserviceRepository {

    private final CredentialsSwitchingPlatform platform;
    private final ApplicationApiRepresentation api;

    public CurrentMicroserviceRepository(CredentialsSwitchingPlatform platform, ApplicationApiRepresentation api) {
        this.platform = platform;
        this.api = api;
    }

    // no registration needed, this is done now during deployment on kubernetes
    @Override
    public ApplicationRepresentation register(final MicroserviceMetadataRepresentation metadata) {
        log.debug("Self registration procedure not activated for current application with {}", metadata);
        final ApplicationRepresentation application = getCurrentApplication();
        if (application == null) {
            throw new SDKException("Failed to load current microservice. Microservice must be configured before running the SDK, please contact administrator");
        } else {
            if (!MICROSERVICE.equals(application.getType())) {
                throw new SDKException("Failed to load current microservice. There is another application with name \"" + application.getName() + "\"");
            }
            return application;
        }
    }

    // no registration needed, this is done now during deployment on kubernetes
    @Override
    @Deprecated//this method will be removed
    public ApplicationRepresentation register(final String applicationNameNotUsed, final MicroserviceMetadataRepresentation metadata) {
        return register(metadata);
    }

    @Override
    public ApplicationRepresentation getCurrentApplication() {
        try {
            return applicationApi().currentApplication().get();
        } catch (final Exception ex) {
            return (ApplicationRepresentation) handleException("GET", api.getCurrentApplication(), ex);
        }
    }

    @Override
    public Iterable<ApplicationUserRepresentation> getSubscriptions() {
        final String url = api.getCurrentApplicationSubscriptions();
        try {
            return rest().get(url, APPLICATION_USER_COLLECTION_MEDIA_TYPE, ApplicationUserCollectionRepresentation.class);
        } catch (final Exception ex) {
            return (ApplicationUserCollectionRepresentation) handleException("GET", url, ex);
        }
    }

    @Override
    public Iterable<ApplicationUserRepresentation> getSubscriptions(String notUsedApplicationId) {
        return getSubscriptions();
    }

    private RestOperations rest() {
        return platform.get();
    }

    protected static Object handleException(String method, String url, Exception ex) {
        if (ex instanceof SDKException) {
            final SDKException sdkException = (SDKException) ex;
            if (sdkException.getHttpStatus() == SC_FORBIDDEN || sdkException.getHttpStatus() == SC_UNAUTHORIZED) {
                log.warn("User has no permission to api " + method + " " + url);
                log.debug("Details :", ex);
                return null;
            } else if (sdkException.getHttpStatus() == SC_NOT_FOUND) {
                log.warn("Not found " + method + " " + url + "(" + ex.getMessage() + ")");
                log.debug("Details :", ex);
                return null;
            }
        }
        throw new SDKException("Error invoking " + method + " " + url, ex);
    }

    private ApplicationApi applicationApi() {
        return new ApplicationApi(platform.get(), api);
    }
}
