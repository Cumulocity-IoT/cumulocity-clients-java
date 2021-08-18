package com.cumulocity.microservice.monitoring.health.controller;

import com.cumulocity.microservice.monitoring.health.controller.configuration.TestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.endpoint.http.ActuatorMediaType;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;


import static io.restassured.http.ContentType.JSON;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS;

@DirtiesContext(classMode = AFTER_CLASS)
@SpringBootTest(classes = TestConfiguration.class )
public class PlatformNotPresentInContextTest {

    @Test
    @WithMockUser(authorities = "ROLE_ACTUATOR")
    public void healthShouldBeUp() {
        given()
                .accept(ActuatorMediaType.V3_JSON).
        when()
                .get("/health").

        then()
                .statusCode(200)
                .contentType(JSON)
                .body("components.platform.status", equalTo(null));
    }
}
