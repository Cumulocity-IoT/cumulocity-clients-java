package com.cumulocity.sdk.client;

import jakarta.ws.rs.client.Client;
import lombok.SneakyThrows;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class CumulocityHttpClientTest {

        protected final String HOST = "http://management.cumulocity.com:8080";

        protected CumulocityHttpClient client;

        protected Client jerseyClient;

        protected ClientConfig clientConfig;

        protected int chunkedEncodingSize = 1024;

        protected  ResponseParser responseParser;

        protected RestConnector restConnector;

        @BeforeEach
        public void setUp() {
            PlatformParameters platformParameters = new PlatformParameters();
            platformParameters.setForceInitialHost(true);
            platformParameters.setHost(HOST);
            platformParameters.setChunkedEncodingSize(chunkedEncodingSize);
            restConnector = new RestConnector(platformParameters,responseParser);
            jerseyClient = restConnector.getClient();
            client = createClient(platformParameters);
        }

        @Test
        public void shouldChangeHostToPlatformHostIfThisIsForcedInParameters() {
            String queryParams = "?test=1&a=1";
            String pathParams = "/test/1/q=1&a=1";

            verifyResolvedPath(HOST, HOST);
            verifyResolvedPath(HOST, "http://127.0.0.1");
            verifyResolvedPath(HOST, "http://127.0.0.1:8181");
            verifyResolvedPath(HOST + queryParams, "http://127.0.0.1:8181" + queryParams);
            verifyResolvedPath(HOST + queryParams, "http://127.0.0.1" + queryParams);
            verifyResolvedPath(HOST + pathParams, "http://127.0.0.1" + pathParams);
        }

        @Test
        public void shouldEnableChunkedEncoding() {
            Integer chunkedProperty = (Integer) jerseyClient.getConfiguration().getProperty(ClientProperties.CHUNKED_ENCODING_SIZE);

            assertThat(chunkedProperty).isEqualTo(chunkedEncodingSize);
        }

        @ParameterizedTest
        @ValueSource(strings = { "{", "}" })
        public void shouldThrowExceptionWithCode400WhenIllegalCharactersInPath(String character) {
            String path = "http://example.com/" + character;
            Throwable thrown = catchThrowable(() -> client.target(path).request());

            assertThat(thrown).isInstanceOf(SDKException.class);
            assertThat(thrown).hasMessageContaining("Illegal characters used in URL.");
            assertThat(((SDKException) thrown).getHttpStatus()).isEqualTo(400);
        }

    @ParameterizedTest
    @CsvSource(value = {
            "http://localhost:8001, https://management.cumulocity.com/inventory/managedObjects, http://localhost:8001/inventory/managedObjects",
            "http://localhost:8001/c8y, https://management.cumulocity.com/inventory/managedObjects, http://localhost:8001/c8y/inventory/managedObjects"
    })
    public void forceInitialHostShouldResolveToBaseUrl(String baseUrl, String requestUrl, String expectedResolvedUrl) {
        resetClientWithInitialHost(baseUrl, true);

        verifyResolvedPath(expectedResolvedUrl, requestUrl);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "http://localhost:8001, http://localhost:8001/inventory/managedObjects, http://localhost:8001/inventory/managedObjects",
            "http://localhost:8001, https://localhost:8001/inventory/managedObjects, http://localhost:8001/inventory/managedObjects",
            "http://localhost:8001/c8y, http://localhost:8001/c8y/inventory/managedObjects, http://localhost:8001/c8y/inventory/managedObjects",
            "http://localhost:8001/c8y, https://localhost:8001/c8y/inventory/managedObjects, http://localhost:8001/c8y/inventory/managedObjects",
            "http://localhost:8001/c8y, http://localhost:8001/inventory/managedObjects, http://localhost:8001/c8y/inventory/managedObjects",
    })
    public void forceInitialHostShouldResolveToBaseUrlForSameDomain(String baseUrl, String requestUrl, String expectedResolvedUrl) {
        resetClientWithInitialHost(baseUrl, true);

        verifyResolvedPath(expectedResolvedUrl, requestUrl);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "http://localhost:8001/, https://management.cumulocity.com/inventory/managedObjects, http://localhost:8001/inventory/managedObjects",
            "http://localhost:8001/, http://localhost:8001/inventory/managedObjects, http://localhost:8001/inventory/managedObjects",
            "http://localhost:8001/c8y/, https://management.cumulocity.com/inventory/managedObjects, http://localhost:8001/c8y/inventory/managedObjects",
            "http://localhost:8001/c8y/, http://localhost:8001/c8y/inventory/managedObjects, http://localhost:8001/c8y/inventory/managedObjects"
    })
    public void forceInitialHostShouldNotDuplicateTrailingSlash(String baseUrl, String requestUrl, String expectedResolvedUrl) {
        resetClientWithInitialHost(baseUrl, true);

        verifyResolvedPath(expectedResolvedUrl, requestUrl);
    }


    @ParameterizedTest
    @CsvSource(value = {
            "http://localhost:8001, https://management.cumulocity.com/inventory/managedObjects?test=1&a=1, http://localhost:8001/inventory/managedObjects?test=1&a=1",
            "http://localhost:8001, http://localhost:8001/inventory/managedObjects?test=1&a=1, http://localhost:8001/inventory/managedObjects?test=1&a=1",
            "http://localhost:8001/c8y, https://management.cumulocity.com/inventory/managedObjects?test=1&a=1, http://localhost:8001/c8y/inventory/managedObjects?test=1&a=1",
            "http://localhost:8001/c8y, http://localhost:8001/c8y/inventory/managedObjects?test=1&a=1, http://localhost:8001/c8y/inventory/managedObjects?test=1&a=1"
    })
    public void forceInitialHostShouldIncludeAllPathParameters(String baseUrl, String requestUrl, String expectedResolvedUrl) {
        resetClientWithInitialHost(baseUrl, true);

        verifyResolvedPath(expectedResolvedUrl, requestUrl);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "http://localhost:8001/c8y/, http://localhost:8001/inventory/managedObjects?test=1&a=1, http://localhost:8001/inventory/managedObjects?test=1&a=1",
            "http://localhost:8001/c8y/, https://management.cumulocity.com/inventory/managedObjects?test=1&a=1, https://management.cumulocity.com/inventory/managedObjects?test=1&a=1",
    })
    public void shouldUseRequestURL(String baseUrl, String requestUrl, String expectedResolvedUrl) {
        resetClientWithInitialHost(baseUrl, false);

        verifyResolvedPath(expectedResolvedUrl, requestUrl);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "true, http://localhost:8001/c8y/, /health, http://localhost:8001/c8y/health",
            "false, http://localhost:8001/c8y/, /health, http://localhost:8001/c8y/health",
    })
    public void shouldPrependBaseURLIfNoHostInRequestURL(boolean forceInitialHost, String baseUrl, String requestUrl, String expectedResolvedUrl) {
        resetClientWithInitialHost(baseUrl, forceInitialHost);

        verifyResolvedPath(expectedResolvedUrl, requestUrl);
    }

    private void resetClientWithInitialHost(String initialHost, boolean forceInitialHost) {
        PlatformParameters platformParameters = new PlatformParameters();
        platformParameters.setForceInitialHost(forceInitialHost);
        platformParameters.setHost(initialHost);

        client = createClient(platformParameters);
    }

    @SneakyThrows
    protected void verifyResolvedPath(String expected, String initial) {
        String resolved = client.resolvePath(initial);
        assertThat(resolved).isEqualTo(expected);
    }

    protected CumulocityHttpClient createClient(PlatformParameters platformParameters) {
        CumulocityHttpClient client = new CumulocityHttpClient(clientConfig);
        client.setPlatformParameters(platformParameters);
        return client;
    }

}
