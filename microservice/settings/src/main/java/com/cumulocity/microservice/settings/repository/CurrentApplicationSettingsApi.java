package com.cumulocity.microservice.settings.repository;

import com.cumulocity.rest.representation.tenant.OptionsRepresentation;
import com.cumulocity.sdk.client.RestOperations;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import jakarta.ws.rs.core.MediaType;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class CurrentApplicationSettingsApi {

    private static final Logger log = LoggerFactory.getLogger(CurrentApplicationSettingsApi.class);

    private static final String CURRENT_APPLICATION_SETTINGS = "application/currentApplication/settings";

    private final RestOperations rest;
    private final Supplier<String> baseUrl;

    public OptionsRepresentation findAll() {
        return SettingsRetry
                .<OptionsRepresentation>with(getMaxIteration(), getBackoffRate())
                .executeWithRetry(
                        () -> rest.get(url(), MediaType.APPLICATION_JSON_TYPE, OptionsRepresentation.class)
                );
    }

    private String url() {
        final String prefix = StringUtils.trimTrailingCharacter(baseUrl.get(), '/');
        return prefix + "/" + CURRENT_APPLICATION_SETTINGS;
    }

    protected int getMaxIteration() {
        return 3;
    }

    protected int getBackoffRate() {
        return 5;
    }

    private static class SettingsRetry<T> {
        private final int maxIterations;
        private final int backoffRate;
        private int currentExecutionNumber;

        private SettingsRetry(int maxIterations, int backoffRate) {
            this.maxIterations = maxIterations;
            this.backoffRate = backoffRate;
            this.currentExecutionNumber = 0;
        }

        public static <T> SettingsRetry<T> with(int maxIterations, int backOfRate) {
            return new SettingsRetry<T>(maxIterations, backOfRate);
        }

        public T executeWithRetry(Supplier<T> supplier) {
            try {
                return execute(supplier);
            } catch (Exception e) {
                if (currentExecutionNumber < maxIterations) {
                    return executeWithWait(supplier, e);
                } else {
                    throw e;
                }
            }
        }

        private T executeWithWait(Supplier<T> supplier, Exception e) {
            log.debug("Error while calling getting microservice platform settings retry will be attempted", e);

            try {
                Thread.sleep(backoffRate * currentExecutionNumber * 1000L);
            } catch (InterruptedException interruptedException) {
                log.error("Error while waiting to retry", e);
            }

            return executeWithRetry(supplier);
        }

        private T execute(Supplier<T> supplier) {
            currentExecutionNumber++;
            return supplier.get();
        }

    }
}
