package com.cumulocity.microservice.lpwan.codec.rest;

import com.cumulocity.microservice.customdecoders.api.service.DecoderService;
import com.cumulocity.microservice.customencoders.api.service.EncoderService;
import io.github.robwin.swagger.test.SwaggerAssertions;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import io.swagger.util.Yaml;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
public class CodecControllerContractTest {

    @EnableSwagger2
    @Import(CodecController.class)
    @EnableAutoConfiguration(exclude = {
            UserDetailsServiceAutoConfiguration.class,
            SecurityAutoConfiguration.class,
            ManagementWebSecurityAutoConfiguration.class
    })
    @SpringBootConfiguration(proxyBeanMethods = false)
    static class TestSwaggerConfiguration {
        @Bean
        Docket apiDocketDecode() {
            return new Docket(DocumentationType.SWAGGER_2)
                    .select()
                    .apis(RequestHandlerSelectors.basePackage("com.cumulocity.microservice.lpwan.codec.rest"))
                    .paths(PathSelectors.any())
                    .build();
        }
    }

    @LocalServerPort
    int port;

    @MockBean
    DecoderService decoderService;

    @MockBean
    EncoderService encoderService;

    @Test
    public void validateThatImplementationSatisfiesConsumerSpecification() throws URISyntaxException {
        String applicationUrl = "http://localhost:" + port;
        File designFirstSwagger = new File("src/test/java/com/cumulocity/microservice/lpwan/codec/rest/goldenContract.yaml");
        SwaggerAssertions.assertThat(applicationUrl + "/v2/api-docs")
                .isEqualTo(designFirstSwagger.getAbsolutePath());
    }

    // Remove the @Ignore annotation and run this test to generate the goldenContract.
    @Ignore
    @Test
    public void generateGoldenContract() throws MalformedURLException {
        String applicationUrl = "http://localhost:" + port;
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
