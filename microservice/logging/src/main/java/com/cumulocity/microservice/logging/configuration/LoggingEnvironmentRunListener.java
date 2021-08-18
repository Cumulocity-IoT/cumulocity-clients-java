package com.cumulocity.microservice.logging.configuration;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import com.cumulocity.microservice.properties.ConfigurationFileProvider;
import com.google.common.collect.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;

import java.io.File;
import java.nio.file.Path;

import static java.nio.file.Files.exists;

@Order(5)
public class LoggingEnvironmentRunListener implements SpringApplicationRunListener {

    private static final Logger log = LoggerFactory.getLogger(LoggingEnvironmentRunListener.class);

    public LoggingEnvironmentRunListener(SpringApplication application, String[] args) {
    }

    @Override
    public void starting() {

    }

    /**
     * for older spring boot versions
     */
    public void started() {
    }

    @Override
    public void environmentPrepared(ConfigurableEnvironment configurableEnvironment) {
        initLogging(new ConfigurationFileProvider(configurableEnvironment));
    }

    @Override
    public void contextPrepared(ConfigurableApplicationContext configurableApplicationContext) {

    }

    @Override
    public void contextLoaded(ConfigurableApplicationContext configurableApplicationContext) {

    }

    @Override
    public void started(ConfigurableApplicationContext context) {

    }

    @Override
    public void running(ConfigurableApplicationContext context) {

    }

    @Override
    public void failed(ConfigurableApplicationContext context, Throwable exception) {

    }

    // temporary for backward compatibility with spring-boot 1.x
    public void finished(ConfigurableApplicationContext context, Throwable exception) {
        failed(context, exception);
    }

    public void initLogging(ConfigurationFileProvider configurationFileProvider) {
        Iterable<Path> paths = Iterables
                .concat(configurationFileProvider.find("-logging.xml", "-server-logging.xml", "-agent-server-logging.xml"),
                        configurationFileProvider.find(new String[]{"logging"}, ".xml"));
        for (Path path : paths) {
            if (path != null && exists(path)) {
                setupLoggingFile(path.toFile());
                return;
            }
        }
    }

    private static File setupLoggingFile(File file) {
        log.info("logging.conf: {}", file.getAbsoluteFile());

        if (!file.getAbsoluteFile().getName().equals(System.getProperty("logging.conf"))) {
            System.setProperty("logging.conf", file.getAbsolutePath());

            final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            try {
                final JoranConfigurator configurator = new JoranConfigurator();
                configurator.setContext(context);
                context.reset();
                configurator.doConfigure(file);
            } catch (final JoranException je) {
                // StatusPrinter will handle this
            }
            StatusPrinter.printInCaseOfErrorsOrWarnings(context);
        }
        return file;
    }

}
