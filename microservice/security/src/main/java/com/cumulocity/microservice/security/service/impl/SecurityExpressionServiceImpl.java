package com.cumulocity.microservice.security.service.impl;

import com.cumulocity.microservice.security.service.SecurityExpressionService;
import com.cumulocity.microservice.subscription.repository.application.ApplicationApi;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Lazy
@Service
@RequiredArgsConstructor
public class SecurityExpressionServiceImpl implements SecurityExpressionService {
    private final ApplicationApi applicationApi;

    @Override
    public boolean isFeatureEnabled(String featureName) {
        return applicationApi.getByName(featureName).isPresent();
    }

    @Override
    public boolean isServiceUser(String service) {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            final String username = ((UserDetails) principal).getUsername();

            return Objects.equals(username, "service_" + service);
        }
        return false;
    }
}
