package com.cumulocity.microservice.security.filter;

import com.cumulocity.microservice.context.ContextService;
import com.cumulocity.microservice.context.credentials.Credentials;
import com.cumulocity.microservice.context.credentials.UserCredentials;
import com.cumulocity.microservice.security.filter.provider.HttpContextProvider;
import com.cumulocity.microservice.security.filter.provider.PostAuthorizationContextProvider;
import com.cumulocity.microservice.security.filter.provider.PreAuthorizationContextProvider;
import com.cumulocity.microservice.security.filter.provider.SpringSecurityContextProvider;
import com.cumulocity.microservice.subscription.service.MicroserviceSubscriptionsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;
import org.springframework.security.core.context.SecurityContext;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@Configuration(proxyBeanMethods = false)
public class PrePostFiltersConfiguration implements EnvironmentAware {

    private PropertyResolver propertyResolver;
    @Autowired
    private ContextService<Credentials> contextService;
    @Autowired
    private ContextService<UserCredentials> userContext;
    @Autowired
    private MicroserviceSubscriptionsService subscriptionsService;

    @Override
    public void setEnvironment(Environment environment) {
        this.propertyResolver = environment;
    }

    @Bean
    public HttpContextProvider httpContextProvider() {
        return new HttpContextProvider();
    }

    @Bean
    public SpringSecurityContextProvider springSecurityContextProvider() {
        return new SpringSecurityContextProvider(userContext, subscriptionsService, propertyResolver.getProperty("application.name", ""));
    }

    @Bean
    public PreAuthenticateServletFilter preAuthenticateServletFilter(List<PreAuthorizationContextProvider<HttpServletRequest>> credentialsResolvers) {
        return new PreAuthenticateServletFilter(credentialsResolvers, contextService);
    }

    /**
     * Disables auto-registration of {@link PreAuthenticateServletFilter} as it is mean for use in {@link org.springframework.security.web.SecurityFilterChain SecurityFilterChain}.
     * https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#howto.webserver.add-servlet-filter-listener.spring-bean.disable
     */
    @Bean
    public FilterRegistrationBean<PreAuthenticateServletFilter> preAuthenticateFilterRegistration(PreAuthenticateServletFilter filter) {
        FilterRegistrationBean<PreAuthenticateServletFilter> registration = new FilterRegistrationBean<>(filter);
        // Disable filter auto detection/registration by Spring
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public PostAuthenticateServletFilter postAuthenticateServletFilter(List<PostAuthorizationContextProvider<SecurityContext>> credentialsResolvers) {
        return new PostAuthenticateServletFilter(credentialsResolvers, contextService);
    }

    /**
     * Disables auto-registration of {@link PostAuthenticateServletFilter} as it is mean for use in {@link org.springframework.security.web.SecurityFilterChain SecurityFilterChain}.
     * https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#howto.webserver.add-servlet-filter-listener.spring-bean.disable
     */
    @Bean
    public FilterRegistrationBean<PostAuthenticateServletFilter> postAuthenticateFilterRegistration(PostAuthenticateServletFilter filter) {
        FilterRegistrationBean<PostAuthenticateServletFilter> registration = new FilterRegistrationBean<>(filter);
        // Disable filter auto detection/registration by Spring
        registration.setEnabled(false);
        return registration;
    }
}
