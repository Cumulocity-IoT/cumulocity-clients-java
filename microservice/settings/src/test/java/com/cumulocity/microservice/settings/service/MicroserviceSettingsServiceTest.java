package com.cumulocity.microservice.settings.service;

import com.cumulocity.microservice.context.ContextService;
import com.cumulocity.microservice.context.credentials.MicroserviceCredentials;
import com.cumulocity.microservice.settings.repository.CurrentApplicationSettingsApi;
import com.cumulocity.microservice.settings.service.impl.MicroserviceSettingsServiceImpl;
import com.cumulocity.microservice.subscription.model.core.PlatformProperties;
import com.cumulocity.rest.representation.tenant.OptionsRepresentation;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.Callable;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MicroserviceSettingsServiceTest {

    @Mock
    private PlatformProperties platformProperties;
    @Mock
    private CurrentApplicationSettingsApi currentApplicationSettingsApi;
    @Mock
    private ContextService<MicroserviceCredentials> contextService;

    private MicroserviceSettingsService microserviceSettingsService;

    @BeforeEach
    public void setUp() {
        doReturn(new OptionsRepresentation()).when(contextService).callWithinContext(any(MicroserviceCredentials.class), any(Callable.class));
        microserviceSettingsService = new MicroserviceSettingsServiceImpl(platformProperties, contextService, currentApplicationSettingsApi);
    }

    @Test
    public void mustRequestTenantOptionsInTenantContext() {
        // given
        doReturn(true).when(contextService).isInContext();
        doReturn(context("t100")).when(contextService).getContext();
        // when
        microserviceSettingsService.getAll();
        // then
        verify(contextService).callWithinContext(argThat(new TenantMatcher("t100")), any(Callable.class));
    }

    @Test
    public void mustFallbackToBootstrapUserWhenNotInContext() {
        // given
        doReturn(false).when(contextService).isInContext();
        doReturn(bootstrapUser()).when(platformProperties).getMicroserviceBoostrapUser();
        // when
        microserviceSettingsService.getAll();
        // then
        verify(contextService).callWithinContext(argThat(new TenantMatcher("management")), any(Callable.class));
    }

    private MicroserviceCredentials context(String tenantId) {
        return MicroserviceCredentials.builder().tenant(tenantId).username("service_app").build();
    }

    private MicroserviceCredentials bootstrapUser() {
        return MicroserviceCredentials.builder()
                .tenant("management")
                .username("servicebootstrap_app")
                .password("paa33word_1")
                .build();
    }

    @RequiredArgsConstructor
    private static class TenantMatcher implements ArgumentMatcher<MicroserviceCredentials> {
        private final String tenantId;
        @Override
        public boolean matches(MicroserviceCredentials credentials) {
            return credentials.getTenant().equals(tenantId);
        }
    }
}
