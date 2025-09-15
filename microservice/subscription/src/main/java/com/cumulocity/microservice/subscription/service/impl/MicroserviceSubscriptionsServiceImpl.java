package com.cumulocity.microservice.subscription.service.impl;

import com.cumulocity.microservice.context.ContextService;
import com.cumulocity.microservice.context.credentials.MicroserviceCredentials;
import com.cumulocity.microservice.subscription.model.MicroserviceMetadataRepresentation;
import com.cumulocity.microservice.subscription.model.MicroserviceSubscriptionAddedEvent;
import com.cumulocity.microservice.subscription.model.MicroserviceSubscriptionRemovedEvent;
import com.cumulocity.microservice.subscription.model.core.PlatformProperties;
import com.cumulocity.microservice.subscription.repository.MicroserviceSubscriptionsRepository;
import com.cumulocity.microservice.subscription.repository.MicroserviceSubscriptionsRepository.Subscriptions;
import com.cumulocity.microservice.subscription.service.MicroserviceSubscriptionsService;
import com.cumulocity.rest.representation.application.ApplicationRepresentation;
import com.google.common.collect.Lists;
import lombok.Synchronized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static com.cumulocity.microservice.subscription.model.core.PlatformProperties.IsolationLevel.PER_TENANT;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.concurrent.CompletableFuture.completedFuture;

@SuppressWarnings("rawtypes")
@Service
public class MicroserviceSubscriptionsServiceImpl implements MicroserviceSubscriptionsService {

    private static final Logger log = LoggerFactory.getLogger(MicroserviceSubscriptionsService.class);

    public interface MicroserviceChangedListener<T> {
        boolean apply(T event) throws Exception;
    }

    private final PlatformProperties properties;
    private final ApplicationEventPublisher eventPublisher;
    private final MicroserviceSubscriptionsRepository repository;
    private final MicroserviceMetadataRepresentation microserviceMetadataRepresentation;
    private final ContextService<MicroserviceCredentials> contextService;

    private volatile CompletableFuture<Void> subscribing = completedFuture(null);
    private final List<MicroserviceCredentials> subscribingCredentials = new CopyOnWriteArrayList<>();

    private volatile boolean registeredSuccessfully = false;

    private final List<MicroserviceChangedListener> listeners = Lists.newArrayList(
            new MicroserviceChangedListener() {
                public boolean apply(Object event) {
                    try {
                        if (event instanceof ApplicationEvent) {
//                            backwards compatibility - in older spring version there was no publishEvent(Object) method
                            eventPublisher.publishEvent((ApplicationEvent) event);
                        } else {
                            eventPublisher.publishEvent(event);
                        }
                        return true;
                    } catch (final Exception ex) {
                        log.error(ex.getMessage(), ex);
                        return false;
                    }
                }
            }
    );

    @Autowired
    public MicroserviceSubscriptionsServiceImpl(
            final PlatformProperties properties,
            final ApplicationEventPublisher eventPublisher,
            final MicroserviceSubscriptionsRepository repository,
            final MicroserviceMetadataRepresentation microserviceMetadataRepresentation,
            final ContextService<MicroserviceCredentials> contextService) {
        this.properties = properties;
        this.eventPublisher = eventPublisher;
        this.repository = repository;
        this.microserviceMetadataRepresentation = microserviceMetadataRepresentation;
        this.contextService = contextService;
    }

    @Synchronized
    public void listen(MicroserviceChangedListener listener) {
        this.listeners.add(listener);
        for (final MicroserviceCredentials user : repository.getCurrentSubscriptions()) {
            invokeAdded(user, listener);
        }
    }

    @Synchronized
    public <T> void listen(final Class<T> clazz, final MicroserviceChangedListener<T> listener) {
        listen((MicroserviceChangedListener<T>) event -> {
            if (clazz.isInstance(event)) {
                return listener.apply((T) event);
            }
            return true;
        });
    }

    @Override
    @Synchronized
    public void subscribe() {
        try {
            subscribing = new CompletableFuture<>();
            subscribingCredentials.clear();

            final ApplicationRepresentation application = registerApplication();
            Subscriptions subscriptions = retrieveSubscriptions(application);

            subscriptions.getRemoved().stream().filter(user -> {
                log("Remove subscription: {}", user);
                for (final MicroserviceChangedListener listener : listeners) {
                    if (!invokeRemoved(user, listener)) {
                        return false;
                    }
                }
                return true;
            }).collect(Collectors.toList());

            final List<MicroserviceCredentials> successfullyAdded = subscriptions.getAdded().stream().filter(user -> {
                log("Add subscription: {}", user);
                MicroserviceCredentials enhancedUser = MicroserviceCredentials.copyOf(user).appKey(application.getKey()).build();
                subscribingCredentials.add(enhancedUser);
                for (final MicroserviceChangedListener listener : listeners) {
                    if (!invokeAdded(enhancedUser, listener)) {
                        subscribingCredentials.remove(enhancedUser);
                        return false;
                    }
                }
                return true;
            }).collect(Collectors.toList());

            // Must be done at the very end of subscription synchronization because this process is very time consuming.
            // Before this process ends the method #getCredentials will return the old state.
            repository.updateCurrentSubscriptions(subscriptions.getAll().stream().filter(user -> {
                if (subscriptions.getAdded().contains(user)) {
                    return successfullyAdded.contains(user);
                }
                return true;
            }).collect(Collectors.toList()));
        } finally {
            subscribingCredentials.clear();
            subscribing.complete(null);
        }
    }

    private Subscriptions retrieveSubscriptions(ApplicationRepresentation application) {
        if (PER_TENANT.equals(properties.getIsolation())) {
            MicroserviceCredentials microserviceCredentials = MicroserviceCredentials.builder()
                    .username(properties.getMicroserviceUser().getUsername())
                    .tenant(properties.getMicroserviceUser().getTenant())
                    .password(properties.getMicroserviceUser().getPassword())
                    .appKey(properties.getMicroserviceUser().getAppKey())
                    .build();

            return repository.diffWithCurrentSubscriptions(singletonList(microserviceCredentials));
        }
        return repository.retrieveSubscriptions(application.getId());
    }

    private ApplicationRepresentation registerApplication() {
        ApplicationRepresentation application = repository.register(properties.getApplicationName(), microserviceMetadataRepresentation)
                .orElseThrow(() -> new IllegalStateException(format("Application %s not found", properties.getApplicationName())));
        registeredSuccessfully = true;
        return application;
    }

    private boolean invokeRemoved(final MicroserviceCredentials user, final MicroserviceChangedListener listener) {
        try {
            return contextService.callWithinContext(user, () -> listener.apply(new MicroserviceSubscriptionRemovedEvent(user.getTenant())));
        } catch (final Exception ex) {
            log.error(ex.getMessage(), ex);
            return false;
        }
    }

    private boolean invokeAdded(final MicroserviceCredentials user, final MicroserviceChangedListener listener) {
        try {
            return contextService.callWithinContext(user, () -> listener.apply(new MicroserviceSubscriptionAddedEvent(user)));
        } catch (final Exception ex) {
            log.error(ex.getMessage(), ex);
            return false;
        }
    }

    //    During subscription synchronization the method will just return old state.
    @Override
    public Collection<MicroserviceCredentials> getAll() {
        return repository.getCurrentSubscriptions();
    }

    //    During subscription synchronization the method will just return old state.
    @Override
    public Optional<MicroserviceCredentials> getCredentials(String tenant) {
        return getCredentialsAsync(tenant).getNow(empty());
    }

    @Override
    public CompletableFuture<Optional<MicroserviceCredentials>> getCredentialsAsync(String tenant) {
        Optional<MicroserviceCredentials> subscription = findCurrentSubscriptionForTenant(tenant);
        if (subscription.isPresent()) {
            return completedFuture(subscription);
        }

        for (final MicroserviceCredentials credentials : subscribingCredentials) {
            if (credentials.getTenant().equals(tenant)) {
                return completedFuture(of(credentials));
            }
        }

        if (subscribing.isDone()) {
            subscribe();
            return completedFuture(findCurrentSubscriptionForTenant(tenant));
        } else {
            return subscribing.thenApply(unused -> findCurrentSubscriptionForTenant(tenant));
        }
    }

    private Optional<MicroserviceCredentials> findCurrentSubscriptionForTenant(String tenant) {
        for (final MicroserviceCredentials subscription : repository.getCurrentSubscriptions()) {
            if (subscription.getTenant().equals(tenant)) {
                return of(subscription);
            }
        }
        return empty();
    }

    @Override
    public String getTenant() {
        return contextService.getContext().getTenant();
    }

    @Override
    public void runForEachTenant(final Runnable runnable) {
        for (final MicroserviceCredentials credentials : getAll()) {
            contextService.runWithinContext(credentials, runnable);
        }
    }

    @Override
    public void runForTenant(String tenant, final Runnable runnable) {
        callForTenant(tenant, (Callable<Void>) () -> {
            runnable.run();
            return null;
        });
    }

    @Override
    public <T> T callForTenant(String tenant, Callable<T> runnable) {
        Optional<MicroserviceCredentials> maybeCredentials = getCredentials(tenant);
        return maybeCredentials
                .map(microserviceCredentials -> contextService.callWithinContext(microserviceCredentials, runnable))
                .orElse(null);
    }

    @Override
    public boolean isRegisteredSuccessfully() {
        return registeredSuccessfully;
    }

    private void log(String s, MicroserviceCredentials user) {
        String newPassword = user.getPassword();
        if (newPassword != null && newPassword.length() > 3) {
            newPassword = newPassword.substring(0, 2) + "*******";
        }
        log.debug(s, user.withPassword(newPassword));
    }
}
