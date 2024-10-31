package com.cumulocity.microservice.security.token;

import com.cumulocity.microservice.context.credentials.UserCredentials;
import com.cumulocity.model.authentication.AuthenticationMethod;
import com.cumulocity.model.authentication.CumulocityOAuthCredentials;
import com.cumulocity.sdk.client.CumulocityAuthenticationFilter;
import com.google.common.collect.Iterables;
import com.nimbusds.jwt.JWT;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.client.Client;

import java.util.Set;

import static com.cumulocity.microservice.security.token.JwtTokenTestsHelper.SAMPLE_XSRF_TOKEN;
import static com.cumulocity.microservice.security.token.JwtTokenTestsHelper.mockedJwtImpl;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CumulocityCoreAuthenticationTest {

    private static final String TENANT_NAME = "test1234";
    private static final String USERNAME = "user1234";

    @Mock
    private JwtCredentials jwtCredentials;
    private JwtTokenAuthentication jwtTokenAuthentication;

    @BeforeEach
    public void setup() {
        jwtTokenAuthentication = new JwtTokenAuthentication(jwtCredentials);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    public void shouldCreateClientWithFilterForCookieAuth() {
        JWT accessToken = mockedJwtImpl();
        JwtAndXsrfTokenCredentials jwtAndXsrfTokenCredentials = new JwtAndXsrfTokenCredentials(accessToken, SAMPLE_XSRF_TOKEN);
        JwtTokenAuthentication jwtTokenAuthenticationWithXsrf = new JwtTokenAuthentication(jwtAndXsrfTokenCredentials);

        Client client = CumulocityCoreAuthenticationClient.createClient(jwtTokenAuthenticationWithXsrf);

        CumulocityOAuthCredentials credentials = retrieveOAuthCredentialsFromFilter(client);
        assertThat(credentials.getAuthenticationMethod()).isEqualTo(AuthenticationMethod.COOKIE);
        assertThat(credentials.getAuthenticationString()).isEqualTo(accessToken.serialize());
        assertThat(credentials.getXsrfToken()).isEqualTo(SAMPLE_XSRF_TOKEN);
    }

    private CumulocityOAuthCredentials retrieveOAuthCredentialsFromFilter(Client client) {
        Set<Object> providers = client.getConfiguration().getInstances();
        Object provider =  Iterables.find(providers, p -> p instanceof CumulocityAuthenticationFilter);
        assertThat(provider).isNotNull();
        CumulocityAuthenticationFilter filterForXsrf = (CumulocityAuthenticationFilter) provider;
        Object object = ReflectionTestUtils.getField(filterForXsrf, "credentials");
        return (CumulocityOAuthCredentials)object;
    }

    @Test
    public void shouldCreateClientWithFilterForHeaderAuth() {
        JWT accessToken = mockedJwtImpl();
        JwtOnlyCredentials jwtOnlyCredentials = new JwtOnlyCredentials(accessToken);
        JwtTokenAuthentication jwtOnlyTokenAuthentication = new JwtTokenAuthentication(jwtOnlyCredentials);

        Client client = CumulocityCoreAuthenticationClient.createClient(jwtOnlyTokenAuthentication);

        CumulocityOAuthCredentials credentials = retrieveOAuthCredentialsFromFilter(client);
        assertThat(credentials.getAuthenticationMethod()).isEqualTo(AuthenticationMethod.HEADER);
        assertThat(credentials.getAuthenticationString()).isEqualTo("Bearer " + accessToken.serialize());
        assertThat(credentials.getXsrfToken()).isEqualTo(null);
    }

    @Test
    public void shouldCreateClientWithRequestFilterForHeaderAuth() {
        JWT accessToken = mockedJwtImpl();
        JwtOnlyCredentials jwtOnlyCredentials = new JwtOnlyCredentials(accessToken);
        JwtTokenAuthentication jwtOnlyTokenAuthentication = new JwtTokenAuthentication(jwtOnlyCredentials);
        mockHttpServletRequest();
        Client client = CumulocityCoreAuthenticationClient.createClient(jwtOnlyTokenAuthentication);

        CumulocityOAuthCredentials credentials = retrieveOAuthCredentialsFromFilter(client);
        assertThat(credentials.getAuthenticationMethod()).isEqualTo(AuthenticationMethod.HEADER);
        assertThat(credentials.getAuthenticationString()).isEqualTo("Bearer " + accessToken.serialize());
        assertThat(credentials.getXsrfToken()).isEqualTo(null);
        assertThatClientRequestHeaderConfigured(client);;
    }

    private void assertThatClientRequestHeaderConfigured(Client client) {
        Set<Object> providers = client.getConfiguration().getInstances();
        Object provider =  Iterables.find(providers, p -> p instanceof CumulocityCoreAuthenticationClient.ForwardedHeaderOnRequestFilter);
        assertThat(provider).isNotNull();
    }

    private void mockHttpServletRequest() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(requestAttributes);
    }

    @Test
    public void shouldUpdateAndAddTokenCredentials() {
        when(jwtCredentials.toUserCredentials(TENANT_NAME, jwtTokenAuthentication)).thenReturn(UserCredentials.builder().tenant(TENANT_NAME).username(USERNAME).build());

        JwtTokenAuthentication updatedToken = CumulocityCoreAuthenticationClient.updateUserCredentials(TENANT_NAME, jwtTokenAuthentication);

        assertThat(updatedToken.getUserCredentials().getTenant()).isEqualTo(TENANT_NAME);
        assertThat(updatedToken.getUserCredentials().getUsername()).isEqualTo(USERNAME);
    }
}
