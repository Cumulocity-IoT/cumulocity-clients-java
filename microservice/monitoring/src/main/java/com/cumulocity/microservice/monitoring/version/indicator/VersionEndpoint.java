package com.cumulocity.microservice.monitoring.version.indicator;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.info.BuildProperties;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

/**
 * Based on Spring Boot <a href="https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.endpoints.info.build-information">build information</a>.
 * See also <a href="https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.build.generate-info">how to generate build information</a>.
 */
@Endpoint(id = "version")
@RequiredArgsConstructor
public class VersionEndpoint {

    private final BuildProperties buildProperties;

    @ReadOperation(produces = TEXT_PLAIN)
    public String version() {
        return buildProperties.getVersion();
    }
}
