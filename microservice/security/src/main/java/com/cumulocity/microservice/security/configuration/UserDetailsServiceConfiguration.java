package com.cumulocity.microservice.security.configuration;

import com.cumulocity.microservice.security.service.RoleService;
import com.cumulocity.microservice.security.service.SecurityExpressionService;
import com.cumulocity.microservice.security.service.SecurityUserDetailsService;
import com.cumulocity.microservice.security.service.impl.RoleServiceImpl;
import com.cumulocity.microservice.security.service.impl.SecurityExpressionServiceImpl;
import com.cumulocity.microservice.subscription.repository.application.ApplicationApi;
import com.cumulocity.model.authentication.CumulocityCredentials;
import com.cumulocity.sdk.client.user.UserApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.userdetails.UserDetailsService;

@Lazy
@Configuration
@ConditionalOnBean(WebSecurityConfiguration.class)
public class UserDetailsServiceConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RoleService roleService(@Qualifier("userUserApi") UserApi userApi) {
        return new RoleServiceImpl(userApi);
    }

    @Bean
    @ConditionalOnMissingBean
    public UserDetailsService userDetailsService(@Qualifier("userCredentials") CumulocityCredentials userCredentials, RoleService roleService) {
        return new SecurityUserDetailsService(userCredentials, roleService);
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityExpressionService securityExpressionService(ApplicationApi applicationApi) {
        return new SecurityExpressionServiceImpl(applicationApi);
    }
}
