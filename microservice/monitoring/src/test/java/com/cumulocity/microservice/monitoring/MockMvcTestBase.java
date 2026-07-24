package com.cumulocity.microservice.monitoring;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@AutoConfigureMockMvc
@ContextConfiguration(classes = MockMvcTestBase.TestConfig.class)
public abstract class MockMvcTestBase {

    @TestConfiguration
    @SpringBootConfiguration
    @EnableAutoConfiguration
    public static class TestConfig {
    }

    @Autowired
    protected WebApplicationContext context;

    @BeforeEach
    public void setUp() {
        RestAssuredMockMvc.mockMvc(MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build());
    }
}
