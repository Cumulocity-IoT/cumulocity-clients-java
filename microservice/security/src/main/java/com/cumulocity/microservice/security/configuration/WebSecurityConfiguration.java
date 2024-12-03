package com.cumulocity.microservice.security.configuration;

import com.cumulocity.microservice.security.filter.PostAuthenticateServletFilter;
import com.cumulocity.microservice.security.filter.PreAuthenticateServletFilter;
import com.cumulocity.microservice.security.filter.PrePostFiltersConfiguration;
import com.cumulocity.microservice.security.token.CumulocityOAuthConfiguration;
import com.cumulocity.microservice.security.token.CumulocityOAuthMicroserviceFilter;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

import static org.springframework.security.config.Customizer.withDefaults;

@Order(99)
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@Import({
        CumulocityOAuthConfiguration.class,
        PrePostFiltersConfiguration.class
})
public class WebSecurityConfiguration {

    @Autowired
    private UserDetailsService userDetailsService;

    @Value("${management.security.roles:TENANT_MANAGEMENT_ADMIN}")
    private String[] securityRolesLoggersActuator;

    @Autowired
    public void configureAuthenticationManager(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService);
    }

    @Bean
    @SuppressWarnings("deprecation")
    public static PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, HandlerMappingIntrospector introspector, ServerProperties serverProperties,
            CumulocityOAuthMicroserviceFilter cumulocityOAuthMicroserviceFilter,
            PreAuthenticateServletFilter preAuthenticateServletFilter,
            PostAuthenticateServletFilter postAuthenticateServletFilter,
            ObjectProvider<Customizer<SessionManagementConfigurer<HttpSecurity>>> sessionManagementConfigurer
    ) throws Exception {

        if (securityRolesLoggersActuator.length == 0) {
            securityRolesLoggersActuator = new String[] {"TENANT_MANAGEMENT_ADMIN"};
        }

        // https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html#match-by-mvc
        MvcRequestMatcher.Builder mvc = new MvcRequestMatcher.Builder(introspector)
                .servletPath(serverProperties.getServlet().getContextPath());

        http
                .authorizeHttpRequests(authorize -> authorize
                        // https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html#match-by-dispatcher-type
                        .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()
                        .requestMatchers(mvc.pattern("/metadata")).permitAll()
                        .requestMatchers(mvc.pattern("/health")).permitAll()
                        .requestMatchers(mvc.pattern("/prometheus")).permitAll()
                        .requestMatchers(mvc.pattern("/metrics")).permitAll()
                        .requestMatchers(mvc.pattern("/version")).permitAll()
                        .requestMatchers(mvc.pattern(HttpMethod.POST, "/loggers/*")).hasAnyRole(securityRolesLoggersActuator)
                        .requestMatchers(mvc.pattern(HttpMethod.POST, "/loggers")).hasAnyRole(securityRolesLoggersActuator)
                        .anyRequest().fullyAuthenticated()
                )
                .httpBasic(withDefaults())
               // .csrf(AbstractHttpConfigurer::disable)
                .csrf((csrf) -> csrf
                        .csrfTokenRequestHandler(new XorCsrfTokenRequestAttributeHandler())
                .securityContext(AbstractHttpConfigurer::disable)
                .sessionManagement(sessionManagementConfigurer.getIfAvailable(() -> AbstractHttpConfigurer::disable))
                .requestCache(AbstractHttpConfigurer::disable);

        http.addFilterBefore(cumulocityOAuthMicroserviceFilter, BasicAuthenticationFilter.class);

        http.addFilterBefore(preAuthenticateServletFilter, BasicAuthenticationFilter.class);
        http.addFilterAfter(postAuthenticateServletFilter, AnonymousAuthenticationFilter.class);

        return http.build();
    }
}
