package com.cumulocity.microservice.security.configuration;

import com.cumulocity.microservice.security.service.SecurityExpressionService;
import com.cumulocity.microservice.security.service.impl.SecurityExpressionServiceImpl;
import com.cumulocity.microservice.subscription.repository.application.ApplicationApi;
import com.cumulocity.rest.representation.application.ApplicationRepresentation;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest(classes = {
        MethodSecurityConfigurationTest.TestConfiguration.class,
        MethodSecurityConfiguration.class
})
public class MethodSecurityConfigurationTest {

    public interface TestService {
        boolean doSomethingWhatRequiresFeatureEnabled();

        boolean doSomethingWhatRequiresServiceUser();

        boolean doSomethingWhatRequiresToBeTenantManagement();
    }

    @Configuration
    public static class TestConfiguration {
        @Bean
        public TestService testService() {
            return new TestService() {
                @PreAuthorize("@c8yAuthz.isFeatureEnabled('feature-cep-custom-rule')")
                public boolean doSomethingWhatRequiresFeatureEnabled() {
                    return true;
                }

                @PreAuthorize("@c8yAuthz.isServiceUser('smartrule')")
                public boolean doSomethingWhatRequiresServiceUser() {
                    return true;
                }

                @PreAuthorize("@c8yAuthz.isCurrentTenantManagement()")
                public boolean doSomethingWhatRequiresToBeTenantManagement() {
                    return true;
                }
            };
        }

        @Bean(name = SecurityExpressionService.BEAN_NAME)
        public SecurityExpressionService securityExpressionService(ApplicationApi applications) {
            return new SecurityExpressionServiceImpl(applications);
        }

        @Bean
        public UserDetailsService userDetailsService() {
            return SecurityTestUtil::fromCumuloUsername;
        }
    }

    @Autowired
    private TestService testService;

    @MockBean
    private ApplicationApi applications;

    @Test
    @WithMockUser
    public void shouldPassValidationOfFeature() {
        when(applications.getByName(anyString())).thenReturn(Optional.of(new ApplicationRepresentation()));

        final boolean result = testService.doSomethingWhatRequiresFeatureEnabled();

        assertThat(result).isTrue();
    }

    @WithMockUser
    @Test
    public void shouldFailValidationOfFeature() {
        when(applications.getByName(anyString())).thenReturn(Optional.<ApplicationRepresentation>empty());

        Throwable throwable = catchThrowable(() -> testService.doSomethingWhatRequiresFeatureEnabled());

        assertThat(throwable).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(username = "service_smartrule")
    public void shouldPassValidationOfServiceUser() {
        final boolean result = testService.doSomethingWhatRequiresServiceUser();

        assertThat(result).isTrue();
    }

    @Test
    @WithMockUser(username = "service_smartrulessssss")
    public void shouldFailValidationOfServiceUser() {
        Throwable throwable = catchThrowable(() -> testService.doSomethingWhatRequiresServiceUser());

        assertThat(throwable).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithUserDetails(value = "t500+/adrian")
    public void shouldFailForOtherTenantNotBeingManagement() {
        Throwable throwable = catchThrowable(() -> testService.doSomethingWhatRequiresToBeTenantManagement());

        assertThat(throwable).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithUserDetails(value = "management/admin")
    public void shouldPassValidationOfTenantManagement() {
        testService.doSomethingWhatRequiresToBeTenantManagement();
    }

}
