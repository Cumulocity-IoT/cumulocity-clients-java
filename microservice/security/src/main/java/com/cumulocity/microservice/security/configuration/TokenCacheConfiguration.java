package com.cumulocity.microservice.security.configuration;

import com.cumulocity.microservice.security.token.JwtAuthenticatedTokenCache;
import com.cumulocity.microservice.security.token.JwtTokenAuthenticationGuavaCache;
import com.cumulocity.microservice.settings.annotation.EnableTenantOptionSettingsConfiguration;
import com.cumulocity.microservice.settings.service.MicroserviceSettingsService;
import com.google.common.primitives.Ints;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Slf4j
@Configuration
@AutoConfigureAfter(EnableTenantOptionSettingsConfiguration.class)
public class TokenCacheConfiguration {

    private final Optional<MicroserviceSettingsService> settingsService;

    public TokenCacheConfiguration(Optional<MicroserviceSettingsService> settingsService) {
        this.settingsService = settingsService;
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtAuthenticatedTokenCache jwtAuthenticatedTokenCache(
            @Value("${cache.guava.maxSize:10000}") int jwtGuavaCacheMaxSize,
            @Value("${cache.guava.expireAfterAccessInMinutes:10}") int jwtGuavaCacheExpireAfterAccess,
            @Value("${jwt.cache.guava.expireInSeconds:0}") int jwtCacheExpireInSeconds) {

        int cacheMaxSize = settingsService
                .map(it -> it.get("cache.guava.maxSize"))
                .map(Ints::tryParse)
                .orElse(jwtGuavaCacheMaxSize);

        int cacheExpireAfterAccess = settingsService
                .map(it -> it.get("cache.guava.expireAfterAccessInMinutes"))
                .map(Ints::tryParse)
                .orElse(jwtGuavaCacheExpireAfterAccess);

        int jwtCacheExpire = settingsService
                .map(it -> it.get("jwt.cache.guava.expireInSeconds"))
                .map(Ints::tryParse)
                .orElse(jwtCacheExpireInSeconds);

        log.info("Default Guava implementation for token cache is used.\nParameters:\n- max cache size: "
                + cacheMaxSize
                + "\n- expire after access: " + cacheExpireAfterAccess + " minutes. Expire after write "
                + jwtCacheExpire + " seconds");

        return new JwtTokenAuthenticationGuavaCache(cacheMaxSize, cacheExpireAfterAccess, jwtCacheExpire);
    }
}


