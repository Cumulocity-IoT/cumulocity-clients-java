package com.cumulocity.microservice.monitoring.health.indicator.subscription;

import com.cumulocity.microservice.subscription.service.MicroserviceSubscriptionsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionHealthIndicator extends AbstractHealthIndicator {

    private final SubscriptionHealthIndicatorProperties configuration;
    private final MicroserviceSubscriptionsService subscriptionsService;

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        if (subscriptionsService.isRegisteredSuccessfully()) {
            builder.up();
        } else {
            builder.down().withDetail("details", "Microservice subscription failed. Check logs.");
        }
        log.debug("health {}", builder.build());
    }

}
