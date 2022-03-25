package com.cumulocity.microservice.lpwan.codec.rest;

import com.cumulocity.microservice.lpwan.codec.sample.Application;
import com.cumulocity.microservice.lpwan.codec.sample.SwaggerConfiguration;
import com.cumulocity.microservice.lpwan.codec.sample.TestBeanConfiguration;
import io.github.robwin.swagger.test.SwaggerAssertions;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import io.swagger.util.Yaml;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@RunWith(SpringRunner.class)
// TestBeanConfiguration is required, as the DecoderService and EncoderService beans are required to startup the application and are required by the CodecController.
@SpringBootTest(classes = {Application.class, TestBeanConfiguration.class, SwaggerConfiguration.class}, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(properties = "server.port=9192")
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
public class CodecControllerContractTest {
    private final String applicationUrl = "http://localhost:9192";

    @Test
    public void validateThatImplementationSatisfiesConsumerSpecification() throws URISyntaxException {
        File designFirstSwagger = new File("src/test/java/com/cumulocity/microservice/lpwan/codec/rest/goldenContract.yaml");
        SwaggerAssertions.assertThat(applicationUrl + "/v2/api-docs")
                .isEqualTo(designFirstSwagger.getAbsolutePath());
    }

    // Remove the @Ignore annotation and run this test to generate the goldenContract.
    @Ignore
    @Test
    public void generateGoldenContract() throws MalformedURLException {
        URL url = new URL(applicationUrl + "/v2/api-docs");
        try(InputStream in = url.openStream()) {
            String contractContents = IOUtils.toString(in, StandardCharsets.UTF_8);
            // Convert obtained JSON contents to YAML Swagger format
            Swagger swagger = new SwaggerParser().parse(contractContents);
            String yamlOutput = Yaml.pretty().writeValueAsString(swagger);
            // write to file
            File file = new File("src/test/java/com/cumulocity/microservice/lpwan/codec/rest/goldenContract.yaml");
            FileUtils.writeStringToFile(file, yamlOutput);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
