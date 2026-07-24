package com.cumulocity.microservice.security.configuration;

import com.cumulocity.microservice.security.controller.ErrorController;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.webmvc.autoconfigure.error.ErrorViewResolver;
import org.springframework.boot.webmvc.error.ErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ErrorControllerConfiguration {
    @Bean
    @ConditionalOnMissingBean(value = org.springframework.boot.webmvc.error.ErrorController.class, search = SearchStrategy.CURRENT)
    public ErrorController errorController(ErrorAttributes errorAttributes, WebProperties webProperties, ObjectProvider<ErrorViewResolver> errorViewResolvers) {
        return new ErrorController(errorAttributes, webProperties, errorViewResolvers);
    }
}
