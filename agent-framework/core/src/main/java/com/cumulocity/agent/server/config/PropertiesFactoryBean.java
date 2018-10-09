package com.cumulocity.agent.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.util.Properties;

import static java.lang.String.format;

public class PropertiesFactoryBean implements FactoryBean<Properties> {

    public static final String DEFAULT_CONFIG_ROOT_DIR = "/etc";

    private static final Logger log = LoggerFactory.getLogger(PropertiesFactoryBean.class);

    private final Environment environment;

    private final ResourceLoader resourceLoader;

    private final Properties properties;

    private final boolean merge;

    private String fileName;

    public PropertiesFactoryBean(String id, String fileName, Environment environment, ResourceLoader resourceLoader) {
        this(id, fileName, environment, resourceLoader, true);
    }

    public PropertiesFactoryBean(String id, String fileName, Environment environment, ResourceLoader resourceLoader, boolean merge) {
        this(id, fileName, environment, resourceLoader, merge, DEFAULT_CONFIG_ROOT_DIR);
    }
    
    public PropertiesFactoryBean(String id, String fileName, Environment environment, ResourceLoader resourceLoader, boolean merge, String configRootDir) {
        this.environment = environment;
        this.resourceLoader = resourceLoader;
        this.properties = loadFromStandardLocations(id, fileName, configRootDir);
        this.merge = merge;
    }

    public Properties getProperties() {
        return properties;
    }

    public String getFileName() {
        return fileName;
    }

    @Override
    public Properties getObject() throws Exception {
        return getProperties();
    }

    @Override
    public Class<?> getObjectType() {
        return Properties.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    private Properties loadFromStandardLocations(String id, String fileName, String configRootDir) {
        return loadFromStandardLocations(id, environment.getRequiredProperty("user.home"), configRootDir, fileName);
    }

    private Properties loadFromStandardLocations(String id, String userHome, String configRootDir, String fileName) {
        return loadFromLocations(
        		format("file:%s/%s/%s-default.properties", configRootDir, id, fileName),
                format("file:%s/%s/%s.properties", configRootDir, id, fileName), 
                format("file:%s/.%s/%s.properties", userHome, id, fileName),
                format("classpath:META-INF/%s/%s.properties", id, fileName), 
                format("classpath:META-INF/spring/%s.properties", fileName));
    }

    private Properties loadFromLocations(String... locations) {
        Properties properties = new Properties();
        for (String location : locations) {
            loadFromLocation(properties, location, resourceLoader);
        }
        appendStandardProperties(properties);
        return properties;
    }

    private void appendStandardProperties(Properties properties) {
        properties.setProperty("endpoints.enabled", "false");
        properties.setProperty("endpoints.health.enabled", "true");
        properties.setProperty("endpoints.metrics.enabled", "true");
        properties.setProperty("endpoints.prometheus.enabled", "true");
        properties.setProperty("endpoints.loggers.enabled", "true");
    }

    private void loadFromLocation(Properties properties, String location, ResourceLoader resourceLoader) {
        log.debug("searching for {}", location);
        Resource resource = resourceLoader.getResource(location);
        if (resource.exists()) {
            log.debug("founded {}", location);
            if (!merge) {
                properties.clear();
            }
            try {
                fileName = resource.getURI().getPath();
                properties.load(resource.getInputStream());
            } catch (IOException e) {
                throw new RuntimeException("Error loading properties!", e);
            }
        }
    }
}
