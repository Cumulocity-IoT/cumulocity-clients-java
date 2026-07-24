package com.cumulocity.microservice.monitoring.health.indicator;

import com.cumulocity.microservice.monitoring.MockMvcTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;

import static io.restassured.http.ContentType.TEXT;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.when;

@DirtiesContext
@AutoConfigureMetrics
@SpringBootTest(classes = TestHealthIndicatorConfiguration.class)
public class PrometheusHealthIndicatorTest extends MockMvcTestBase {

    @Test
    @WithMockUser(authorities = "ROLE_ACTUATOR")
    public void prometheusShouldBeUp() {
        when()
                .get("/prometheus").

        then()
                .statusCode(200)
                .contentType(TEXT);
    }

    @Test
    public void prometheusShouldBeSecured() {
        when()
                .get("/prometheus").

        then()
                .statusCode(401);
    }
}
