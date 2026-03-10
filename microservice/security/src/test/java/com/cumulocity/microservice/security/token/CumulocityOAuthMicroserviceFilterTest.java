package com.cumulocity.microservice.security.token;

import com.cumulocity.microservice.context.ContextService;
import com.cumulocity.microservice.context.credentials.UserCredentials;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

import static com.cumulocity.microservice.security.token.CookieReader.AUTHORIZATION_KEY;
import static com.cumulocity.microservice.security.token.CumulocityCoreAuthenticationClient.ForwardedHeaderOnRequestFilter.X_FORWARDED_HOST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.Mockito.*;

public class CumulocityOAuthMicroserviceFilterTest {

    private final static String SAMPLE_TOKEN = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOm51bGwsImlzcyI6ImN1bXVsb2NpdHkuZGVmYXVsdC5zdmMuY2x1c3Rlci5sb2NhbCIsImF1ZCI6ImN1bXVsb2NpdHkuZGVmYXVsdC5zdmMuY2x1c3Rlci5sb2NhbCIsInN1YiI6ImFkbWluIiwidGNpIjoiZDMwMTczNjYtY2Y3Yi00MjdlLWE2OTMtNzJiYjg2MGE5MDgzIiwiaWF0IjoxNTY1NzYxMTg0LCJuYmYiOjE1NjU3NjExODQsImV4cCI6MTU2Njk3MDc4NCwidGZhIjpmYWxzZSwidGVuIjoibWFuYWdlbWVudCIsInhzcmZUb2tlbiI6InZ2VXlpS3h6c1VHQlhNbGNPb2RrIn0.TDz9k0NfKeLK5f0dwZ_gqOWyweMLpaIdEtU6snos9_0ephtI4HibCVEOV9JPoHZnaqjAUyfmhQc7WN2JLpMX6Q";
    private final static String SAMPLE_X_XSRF_TOKEN = "vvUyiKxzsUGBXMlcOodk";

    private CumulocityOAuthMicroserviceFilter filter;
    private AuthenticationEntryPoint authenticationEntryPoint;
    private ContextService<UserCredentials> contextService;

    private AuthenticationManager authenticationManager;

    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final HttpServletResponse response = new MockHttpServletResponse();
    private FilterChain chain;
    private UserCredentials userCredentials;


    @BeforeEach
    public void setup() {
        chain = mock(FilterChain.class);
        authenticationEntryPoint = mock(AuthenticationEntryPoint.class);
        this.userCredentials = UserCredentials.builder().build();

        authenticationManager = mock(AuthenticationManager.class);
        contextService = mock(ContextService.class);
        filter = new CumulocityOAuthMicroserviceFilter(authenticationManager, authenticationEntryPoint, contextService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void shouldAuthenticateWithAuthorizationBearer() throws IOException, ServletException {
        request.addHeader("Authorization", "Bearer " + SAMPLE_TOKEN);
        mockSuccessAuthentication();
        mockContextServiceInvokeRunnable();

        filter.doFilter(request, response, chain);

        JwtTokenAuthentication authentication = (JwtTokenAuthentication) SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication.getCredentials().getJwt().serialize()).isEqualTo(SAMPLE_TOKEN);
        verify(chain).doFilter(request, response);
        verify(contextService).runWithinContext(any(UserCredentials.class), any(Runnable.class));
    }

    @Test
    public void shouldAuthenticateWithAuthorizationCookie() throws IOException, ServletException {
        Cookie[] cookies = {new Cookie(AUTHORIZATION_KEY, SAMPLE_TOKEN)};
        request.setCookies(cookies);
        request.addHeader("X-XSRF-TOKEN", SAMPLE_X_XSRF_TOKEN);
        mockSuccessAuthentication();
        mockContextServiceInvokeRunnable();

        filter.doFilter(request, response, chain);

        JwtTokenAuthentication actualAuthentication = (JwtTokenAuthentication) SecurityContextHolder.getContext().getAuthentication();
        JwtAndXsrfTokenCredentials credentials = (JwtAndXsrfTokenCredentials) actualAuthentication.getCredentials();
        assertThat(credentials.getJwt().serialize()).isEqualTo(SAMPLE_TOKEN);
        assertThat(credentials.getXsrfToken()).isEqualTo(SAMPLE_X_XSRF_TOKEN);
        verify(chain).doFilter(request, response);
        verify(contextService).runWithinContext(same(userCredentials), any(Runnable.class));
    }

    @Test
    public void shouldSetRequestOrigin() throws IOException, ServletException {
        // Given
        request.addHeader("Authorization", "Bearer " + SAMPLE_TOKEN);
        request.addHeader(X_FORWARDED_HOST, "tenant.cumulocity.com");
        mockSuccessAuthentication();
        // When
        filter.doFilter(request, response, chain);
        // Then
        JwtTokenAuthentication authentication = (JwtTokenAuthentication) SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication.getUserCredentials().getOrigin()).isEqualTo("tenant.cumulocity.com");
    }

    @Test
    public void shouldNotAuthenticateWhenNoAuthenticationProvided() throws IOException, ServletException {
        filter.doFilter(request, response, chain);

        verify(authenticationManager, times(0)).authenticate(any(Authentication.class));

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    public void shouldHandleException() throws IOException, ServletException {
        request.addHeader("Authorization", "Bearer " + SAMPLE_TOKEN);
        AuthenticationException exception = new UsernameNotFoundException("");
        when(authenticationManager.authenticate(any(Authentication.class))).thenThrow(exception);

        filter.doFilter(request, response, chain);

        verify(authenticationEntryPoint).commence(request, response, exception);
        verify(chain, times(0)).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    public void shouldNotAuthenticateIfAuthenticationInContext() throws IOException, ServletException {
        request.addHeader("Authorization", "Bearer " + SAMPLE_TOKEN);

        Authentication authentication = mock(AnonymousAuthenticationToken.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filter.doFilter(request, response, chain);

        verify(authenticationManager, times(0)).authenticate(any(Authentication.class));
        verify(chain, times(1)).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(authentication);
    }


    private void mockSuccessAuthentication() {
        when(authenticationManager.authenticate(any(JwtTokenAuthentication.class))).thenAnswer((Answer<JwtTokenAuthentication>) invocation -> {
            JwtTokenAuthentication jwtTokenAuthentication = invocation.getArgument(0);
            jwtTokenAuthentication.setUserCredentials(userCredentials);
            return jwtTokenAuthentication;
        });
    }

    private void mockContextServiceInvokeRunnable() {
        doAnswer(invocationOnMock -> {
            Runnable runnableObject = (Runnable)invocationOnMock.getArguments()[1];
            runnableObject.run();
            return null;
        }).when(contextService).runWithinContext(or(any(UserCredentials.class), Mockito.isNull()), any(Runnable.class));
    }
}
