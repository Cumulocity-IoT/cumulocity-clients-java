package com.cumulocity.microservice.properties;

import org.springframework.boot.env.DefaultPropertiesPropertySource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import static java.util.stream.Collectors.toMap;

@Order
public class MicroserviceDefaultPropertiesRegistrar implements EnvironmentPostProcessor {

    public static final String MICROSERVICE_DEFAULT_PROPERTIES_LOCATION = "classpath:microservice_default.properties";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Properties properties = loadProperties(application.getClassLoader());
        Map<String, Object> propertiesMap = properties.entrySet().stream()
                .collect(toMap(p -> String.valueOf(p.getKey()), Map.Entry::getValue));
        DefaultPropertiesPropertySource.addOrMerge(propertiesMap, environment.getPropertySources());
    }

    private static Properties loadProperties(ClassLoader classLoader) {
        try {
            ResourceLoader resourceLoader = new DefaultResourceLoader(classLoader);
            Resource resource = resourceLoader.getResource(MICROSERVICE_DEFAULT_PROPERTIES_LOCATION);
            return PropertiesLoaderUtils.loadProperties(resource);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load microservice default properties", e);
        }
    }
}
