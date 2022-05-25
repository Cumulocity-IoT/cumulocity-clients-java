package com.cumulocity.microservice.settings.service.impl;

import com.cumulocity.microservice.context.ContextService;
import com.cumulocity.microservice.context.credentials.MicroserviceCredentials;
import com.cumulocity.microservice.settings.repository.CurrentApplicationSettingsApi;
import com.cumulocity.microservice.settings.service.MicroserviceSettingsService;
import com.cumulocity.microservice.subscription.model.core.PlatformProperties;

import com.cumulocity.rest.representation.tenant.OptionsRepresentation;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import static java.util.concurrent.TimeUnit.MINUTES;

@Slf4j
@RequiredArgsConstructor
public class MicroserviceSettingsServiceImpl implements MicroserviceSettingsService {

    private final PlatformProperties platformProperties;
    private final ContextService<MicroserviceCredentials> contextService;
    private final CurrentApplicationSettingsApi settingsApi;

    private final Cache<String, Map<String, String>> cachedSettings = CacheBuilder.newBuilder().expireAfterWrite(10, MINUTES).build();

    @Override
    public Map<String, String> getAll() {
        final MicroserviceCredentials credentials = getCurrentCredentials();
        try {
            return cachedSettings.get(credentials.getTenant(), new Callable<Map<String, String>>() {
                public Map<String, String> call() {
                    log.debug("Loading tenant option settings for tenant: {}", credentials.getTenant());
                    if (isBootstrapUser(credentials.getUsername())) {
                        log.warn("Loading tenant options using bootstrap credentials!");
                    }
                    return toMap(contextService.callWithinContext(credentials, new Callable<OptionsRepresentation>() {
                        public OptionsRepresentation call() {
                            return settingsApi.findAll();
                        }
                    }));
                }
            });
        } catch (ExecutionException e) {
            log.error("Loading tenant option settings failed: {}", e.getMessage());
            log.debug("Loading tenant option settings failed: {}", e.getMessage(), e);
        }
        return Collections.emptyMap();
    }

    @Override
    public <T> T getAs(Class<T> clazz) {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.convertValue(getAll(), clazz);
    }

    private Map<String, String> toMap(OptionsRepresentation settings) {
        Map<String, String> settingsMap = Maps.newHashMap();
        for (String key : settings.propertyNames()) {
            settingsMap.put(key, settings.getProperty(key));
        }
        return settingsMap;
    }

    @Override
    public String getCredential(@NonNull String key) {
        if (!key.startsWith("credentials")) {
            key = "credentials." + key;
        }
        return get(key);
    }

    @Override
    public String get(@NonNull String key) {
        return getAll().get(key);
    }

    @Override
    public void evict(){
        final MicroserviceCredentials credentials = getCurrentCredentials();
        cachedSettings.invalidate(credentials.getTenant());
    }

    private MicroserviceCredentials getCurrentCredentials() {
        if (contextService.isInContext()) {
            return contextService.getContext();
        } else {
            return (MicroserviceCredentials) platformProperties.getMicroserviceBoostrapUser();
        }
    }

    private boolean isBootstrapUser(String username) {
        return username.startsWith("servicebootstrap_");
    }
}
