package com.cumulocity.microservice.security.token;

import com.cumulocity.microservice.context.credentials.Credentials;
import com.cumulocity.microservice.context.credentials.MicroserviceCredentials;
import com.cumulocity.microservice.subscription.service.MicroserviceSubscriptionsService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class OAuthPostAuthorizationContextProviderTest {

    private static final String MY_APPLICATION_NAME = "my-application-name";
    private static final String MY_TENANT_NAME = "my-tenant-name";

    @Mock
    private MicroserviceSubscriptionsService subscriptionService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Mock
    private JwtTokenAuthentication jwtTokenAuthentication;

    @Mock
    private MicroserviceCredentials microserviceCredentials;

    // under tests
    private OAuthPostAuthorizationContextProvider provider;

    @BeforeEach
    public void setUp() {
        this.provider = new OAuthPostAuthorizationContextProvider(MY_APPLICATION_NAME);
        this.provider.setSubscriptionsService(subscriptionService);
    }

    @Test
    public void shouldNotSupportNullSecurityContext() {
        //when
        boolean supports = provider.supports(null);

        //then
        assertThat(supports).isFalse();
    }

    @Test
    public void shouldNotSupportSecurityContextWithNullAuthorization() {
        //given
        given(securityContext.getAuthentication()).willReturn(null);

        //when
        boolean supports = provider.supports(securityContext);

        //then
        assertThat(supports).isFalse();
    }

    @Test
    public void shouldNotSupportNonJwtAuthenticationWithNullAuthorization() {
        //given
        given(securityContext.getAuthentication()).willReturn(authentication);

        //when
        boolean supports = provider.supports(securityContext);

        //then
        assertThat(supports).isFalse();
    }

    @Test
    public void shouldSupportTokenBasedAuthorization() {
        //given
        given(securityContext.getAuthentication()).willReturn(jwtTokenAuthentication);

        //when
        boolean supports = provider.supports(securityContext);

        //then
        assertThat(supports).isTrue();
    }

    @Test
    public void shouldNotSupportAdditionalContextProvidersWhenSubscriptionServiceIsNotAvailable() {
        //given
        OAuthPostAuthorizationContextProvider provider = new OAuthPostAuthorizationContextProvider(MY_APPLICATION_NAME);

        //when
        boolean supports = provider.supports(securityContext);

        //then
        assertThat(supports).isFalse();
    }

    @Test
    public void shouldProvideMicroserviceCredentialsForContext() {
        //given
        given(securityContext.getAuthentication()).willReturn(jwtTokenAuthentication);
        given(jwtTokenAuthentication.getTenantName()).willReturn(MY_TENANT_NAME);
        given(subscriptionService.getCredentials(MY_TENANT_NAME)).willReturn(Optional.of(microserviceCredentials));

        //when
        Credentials credentials = provider.get(securityContext);

        //then
        assertThat(credentials).isNotNull().isSameAs(microserviceCredentials);
    }

    @Test
    public void shouldNotProvideMicroserviceCredentialsWhenTenantNotSubscribeToMicroservice() {
        //given
        given(securityContext.getAuthentication()).willReturn(jwtTokenAuthentication);
        given(jwtTokenAuthentication.getTenantName()).willReturn(MY_TENANT_NAME);
        given(subscriptionService.getCredentials(MY_TENANT_NAME)).willReturn(Optional.empty());

        assertThatThrownBy(() -> {
            //when
            provider.get(securityContext);
            //then
        }).isInstanceOf(AccessDeniedException.class);
    }

}
