package com.cumulocity.microservice.security.configuration;

import com.cumulocity.microservice.security.controller.ErrorController;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorViewResolver;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ErrorControllerConfiguration {
    @Bean
    @ConditionalOnMissingBean(value = org.springframework.boot.web.servlet.error.ErrorController.class, search = SearchStrategy.CURRENT)
    public ErrorController errorController(ErrorAttributes errorAttributes, ServerProperties serverProperties, ObjectProvider<ErrorViewResolver> errorViewResolvers) {
        return new ErrorController(errorAttributes, serverProperties, errorViewResolvers);
    }
}
