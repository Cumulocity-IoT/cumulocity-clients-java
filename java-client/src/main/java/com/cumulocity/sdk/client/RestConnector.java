/*
 * Copyright (C) 2013 Cumulocity GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.cumulocity.sdk.client;

import com.cumulocity.rest.representation.CumulocityMediaType;
import com.cumulocity.rest.representation.ResourceRepresentation;
import com.cumulocity.rest.representation.ResourceRepresentationWithId;
import com.cumulocity.rest.representation.inventory.InventoryMediaType;
import com.cumulocity.sdk.client.buffering.BufferRequestService;
import com.cumulocity.sdk.client.buffering.BufferedRequest;
import com.cumulocity.sdk.client.buffering.Future;
import com.cumulocity.sdk.client.interceptor.BufferedResponseStreamInterceptor;
import com.cumulocity.sdk.client.interceptor.HttpClientInterceptor;
import com.cumulocity.sdk.client.rest.mediatypes.ErrorMessageRepresentationReader;
import com.cumulocity.sdk.client.rest.providers.CumulocityJSONMessageBodyReader;
import com.cumulocity.sdk.client.rest.providers.CumulocityJSONMessageBodyWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.client.*;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import static com.cumulocity.sdk.client.util.StringUtils.isNotBlank;
import static jakarta.ws.rs.core.MediaType.MULTIPART_FORM_DATA_TYPE;
import static jakarta.ws.rs.core.Response.Status.*;
import static org.glassfish.jersey.media.multipart.Boundary.addBoundary;

@Slf4j
public class RestConnector implements RestOperations {

    public static final String X_CUMULOCITY_APPLICATION_KEY = "X-Cumulocity-Application-Key";

    public static final String X_CUMULOCITY_REQUEST_ORIGIN = "X-Cumulocity-Request-Origin";

    public static final String MIME_VERSION = "MIME-Version";

    protected static final String TFA_TOKEN_HEADER = "TFAToken";

    private final PlatformParameters platformParameters;

    private final Client client;

    private final ResponseParser responseParser;

    public RestConnector(PlatformParameters platformParameters, ResponseParser responseParser) {
        this(platformParameters, responseParser, createClient(platformParameters));
    }

    protected RestConnector(PlatformParameters platformParameters, ResponseParser responseParser, Client client) {
        this.platformParameters = platformParameters;
        this.responseParser = responseParser;
        this.client = client;
    }

    public PlatformParameters getPlatformParameters() {
        return platformParameters;
    }

    public Client getClient() {
        return client;
    }

    public ResponseParser getResponseParser() {
        return responseParser;
    }

    @Override
    public void close() {
        if (client != null) {
            client.close();
        }
    }

    public boolean isClosed() {
        if (client instanceof JerseyClient) {
            return ((JerseyClient) client).isClosed();
        }
        return false;
    }

    @Override
    public <T extends ResourceRepresentation> T get(String path, CumulocityMediaType mediaType, Class<T> responseType) throws SDKException {
        Response response = getClientResponse(path, mediaType);
        return responseParser.parse(response, responseType, OK.getStatusCode());
    }

    @Override
    public <T extends Object> T get(String path, MediaType mediaType, Class<T> responseType) throws SDKException {
        Response response = getClientResponse(path, mediaType);
        return responseParser.parseObject(response, OK.getStatusCode(), responseType);
    }

    public Response get(String path, MediaType mediaType) {
        return getClientResponse(path, mediaType);
    }

    @Override
    public Response.Status getStatus(String path, CumulocityMediaType mediaType) throws SDKException {
        Response response = getClientResponse(path, mediaType);
        return response.getStatusInfo().toEnum();
    }

    private Response getClientResponse(String path, MediaType mediaType) {
        Builder builder = client.target(path).request();
        builder = addApplicationKeyHeader(builder);
        builder = addTfaHeader(builder);
        builder = addRequestOriginHeader(builder);
        builder = applyInterceptors(builder);
        builder = addAcceptHeader(builder, mediaType);
        return builder.get();
    }

    @Override
    public <T extends ResourceRepresentation> T postStream(String path, CumulocityMediaType mediaType, InputStream content,
                                                           Class<T> responseClass) throws SDKException {
        Builder builder = client.target(path).request();
        builder = addApplicationKeyHeader(builder);
        builder = addTfaHeader(builder);
        builder = addRequestOriginHeader(builder);
        builder = applyInterceptors(builder);
        builder = addAcceptHeader(builder, mediaType);
        builder = addMimeVersion(builder);
        FormDataMultiPart form = new FormDataMultiPart();
        form.bodyPart(new FormDataBodyPart("file", content, MediaType.APPLICATION_OCTET_STREAM_TYPE));
        return parseResponseWithoutId(responseClass, builder.post(Entity.entity(form, addBoundary(MULTIPART_FORM_DATA_TYPE))), CREATED.getStatusCode());
    }

    @Override
    public <T extends ResourceRepresentation> T postText(String path, String content, Class<T> responseClass) {
        Builder builder = client.target(path).request();
        builder = addDefaultAcceptHeader(builder);
        builder = addApplicationKeyHeader(builder);
        builder = addTfaHeader(builder);
        return parseResponseWithoutId(responseClass, builder.post(Entity.text(content)), CREATED.getStatusCode());
    }

    @Override
    public <T extends ResourceRepresentation> T putText(String path, String content, Class<T> responseClass) {
        Builder builder = client.target(path).request();
        builder = addDefaultAcceptHeader(builder);
        builder = addApplicationKeyHeader(builder);
        builder = addTfaHeader(builder);
        return parseResponseWithoutId(responseClass, builder.put(Entity.text(content)), OK.getStatusCode());
    }

    @Override
    public <T extends ResourceRepresentation> T putStream(String path, String contentType, InputStream content,
                                                          Class<T> responseClass) {
        Builder builder = client.target(path).request();
        builder = addApplicationKeyHeader(builder);
        builder = addTfaHeader(builder);
        builder = addRequestOriginHeader(builder);
        builder = applyInterceptors(builder);
        builder = addAcceptHeader(builder, MediaType.valueOf(InventoryMediaType.MANAGED_OBJECT_TYPE));
        Entity<?> stream = Entity.entity(content, contentType);
        return parseResponseWithoutId(responseClass, builder.put(stream), CREATED.getStatusCode());
    }

    @Override
    public <T extends ResourceRepresentation> T putStream(String path, MediaType mediaType, InputStream content,
                                                          Class<T> responseClass) {
        Builder builder = getResourceBuilder(path);
        builder = addAcceptHeader(builder, mediaType);
        FormDataMultiPart form = new FormDataMultiPart();
        form.bodyPart(new FormDataBodyPart("file", content, MediaType.APPLICATION_OCTET_STREAM_TYPE));
        Entity<MultiPart> stream = Entity.entity(form, form.getMediaType());
        return parseResponseWithoutId(responseClass, builder.put(stream), OK.getStatusCode());
    }

    @Override
    public void postStream(String path, InputStream inputStream, MediaType inputStreamMediaType) {
        Builder builder = getResourceBuilder(path);
        FormDataMultiPart form = new FormDataMultiPart();
        form.bodyPart(new FormDataBodyPart("file", inputStream, inputStreamMediaType));
        Response response = builder.post(Entity.entity(form, addBoundary(form.getMediaType())));
        responseParser.checkStatus(response, CREATED.getStatusCode(), ACCEPTED.getStatusCode(), PARTIAL_CONTENT.getStatusCode());
    }

    @Override
    public <T extends ResourceRepresentation> T postFile(String path, T representation, byte[] bytes, Class<T> responseClass) {
        return postFile(path, representation, bytes, MediaType.APPLICATION_OCTET_STREAM_TYPE, responseClass);
    }

    @Override
    public <T extends ResourceRepresentation> T postFile(String path, T representation, byte[] bytes, MediaType mediaType,
                                                         Class<T> responseClass) {
        Builder builder = getResourceBuilder(path);
        FormDataMultiPart form = new FormDataMultiPart();
        form.bodyPart(new FormDataBodyPart("object", representation, MediaType.APPLICATION_JSON_TYPE));
        form.bodyPart(new FormDataBodyPart("filesize", String.valueOf(bytes.length)));
        form.bodyPart(new FormDataBodyPart("file", bytes, mediaType));
        Entity<MultiPart> file = Entity.entity(form, addBoundary(form.getMediaType()));
        return parseResponseWithoutId(responseClass, builder.post(file), CREATED.getStatusCode());
    }

    @Override
    public <T extends ResourceRepresentation> T postFileAsStream(String path, T representation, InputStream inputStream, Class<T> responseClass) {
        return postFileAsStream(path, representation, inputStream, MediaType.APPLICATION_OCTET_STREAM_TYPE, responseClass);
    }

    @Override
    public <T extends ResourceRepresentation> T postFileAsStream(String path, T representation,
                                                                 InputStream inputStream, MediaType mediaType, Class<T> responseClass) {
        Builder builder = getResourceBuilder(path);
        FormDataMultiPart form = new FormDataMultiPart();
        form.bodyPart(new FormDataBodyPart("object", representation, MediaType.APPLICATION_JSON_TYPE));
        form.bodyPart(new FormDataBodyPart("file", inputStream, mediaType));
        Entity<MultiPart> stream = Entity.entity(form, addBoundary(form.getMediaType()));
        return parseResponseWithoutId(responseClass, builder.post(stream), CREATED.getStatusCode());
    }

    @Override
    public <T extends ResourceRepresentationWithId> T put(String path, MediaType mediaType, T representation) throws SDKException {
        Response response = httpPut(path, mediaType, representation);
        return parseResponseWithId(representation, response, OK.getStatusCode());
    }

    private <T extends ResourceRepresentationWithId> T parseResponseWithId(T representation, Response response, int responseCode)
            throws SDKException {
        @SuppressWarnings("unchecked")
        T repFromPlatform = responseParser.parse(response, (Class<T>) representation.getClass(), responseCode);
        T repToReturn = isDefined(repFromPlatform) ? repFromPlatform : representation;
        if (response.getLocation() != null) {
            repToReturn.setId(responseParser.parseIdFromLocation(response));
        }
        return repToReturn;
    }

    private <T extends ResourceRepresentationWithId> boolean isDefined(T repFromPlatform) {
        return repFromPlatform != null;
    }

    @Override
    public <T extends ResourceRepresentation> Future postAsync(String path, CumulocityMediaType mediaType, T representation)
            throws SDKException {
        return sendAsyncRequest(HttpMethod.POST, path, mediaType, representation);
    }

    @Override
    public <T extends ResourceRepresentation> Future putAsync(String path, CumulocityMediaType mediaType, T representation)
            throws SDKException {
        return sendAsyncRequest(HttpMethod.PUT, path, mediaType, representation);
    }

    private <T extends ResourceRepresentation> Future sendAsyncRequest(String method, String path, CumulocityMediaType mediaType,
                                                                       T representation) {
        BufferRequestService bufferRequestService = platformParameters.createBufferRequestService();
        return bufferRequestService.create(BufferedRequest.create(method, path, mediaType, representation));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ResourceRepresentation> T post(String path, MediaType mediaType, T representation) throws SDKException {
        Response response = httpPost(path, mediaType, mediaType, representation);
        return (T) parseResponseWithoutId(representation.getClass(), response, CREATED.getStatusCode(), OK.getStatusCode());
    }

    @Override
    public <T extends ResourceRepresentationWithId> T post(String path, MediaType mediaType, T representation) throws SDKException {
        Response response = httpPost(path, mediaType, mediaType, representation);
        return parseResponseWithId(representation, response, CREATED.getStatusCode());
    }

    @Override
    public <T extends ResourceRepresentation> void postWithoutResponse(String path, MediaType mediaType, T representation) throws SDKException {
        Builder builder = client.target(path).request();
        builder = addDefaultAcceptHeader(builder);
        builder = addApplicationKeyHeader(builder);
        builder = addTfaHeader(builder);
        builder = addRequestOriginHeader(builder);
        builder = applyInterceptors(builder);
        Entity<T> request = Entity.entity(representation, mediaType);
        Response response = builder.post(request);
        responseParser.checkStatus(response, CREATED.getStatusCode(), ACCEPTED.getStatusCode());
    }

    @Override
    public <Result extends ResourceRepresentation, Param extends ResourceRepresentation> Result post(
            final String path,
            final CumulocityMediaType contentType,
            final CumulocityMediaType accept,
            final Param representation,
            final Class<Result> clazz) {
        Response response = httpPost(path, contentType, accept, representation);
        return parseResponseWithoutId(clazz, response, OK.getStatusCode(), CREATED.getStatusCode());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ResourceRepresentation> T put(String path, MediaType mediaType, T representation) throws SDKException {

        Response response = httpPut(path, mediaType, representation);
        return (T) parseResponseWithoutId(representation.getClass(), response, OK.getStatusCode());
    }

    private Builder addApplicationKeyHeader(Builder builder) {
        if (isNotBlank(platformParameters.getApplicationKey())) {
            builder = builder.header(X_CUMULOCITY_APPLICATION_KEY, platformParameters.getApplicationKey());
        }
        return builder;
    }

    private Builder addTfaHeader(Builder builder) {
        if (isNotBlank(platformParameters.getTfaToken())) {
            builder = builder.header(TFA_TOKEN_HEADER, platformParameters.getTfaToken());
        }
        return builder;
    }

    private Builder addRequestOriginHeader(Builder builder) {
        if (isNotBlank(platformParameters.getRequestOrigin())) {
            builder = builder.header(X_CUMULOCITY_REQUEST_ORIGIN, platformParameters.getRequestOrigin());
        }
        return builder;
    }

    private Builder applyInterceptors(Builder builder) {
        if (platformParameters.interceptorSet != null) {
            synchronized (platformParameters.interceptorSet) {
                for (HttpClientInterceptor interceptor : platformParameters.interceptorSet) {
                    builder = interceptor.apply(builder);
                }
            }
        }
        return builder;
    }

    private Builder addAcceptHeader(Builder builder, MediaType accept) {
        if (platformParameters.requireResponseBody()) {
            return builder.accept(accept);
        }
        return addDefaultAcceptHeader(builder);
    }

    private Builder addDefaultAcceptHeader(Builder builder) {
        // for backward compatibility avoid jersey 2.x setting default accept header
        // first call to `header()` with null value removes default accept header
        builder = builder.header(HttpHeaders.ACCEPT, null);
        return builder.accept(CumulocityMediaType.WILDCARD);
    }

    private Builder addMimeVersion(Builder builder) {
        builder.header(MIME_VERSION, "1.0");
        return builder;
    }

    private <T extends ResourceRepresentation> T parseResponseWithoutId(Class<T> type, Response response, int... responseCodes)
            throws SDKException {
        return responseParser.parse(response, type, responseCodes);
    }

    private <T extends ResourceRepresentation> Response httpPost(String path, MediaType contentType, MediaType accept, T representation) {
        Builder builder = client.target(path).request();

        builder = addAcceptHeader(builder, accept);
        builder = addApplicationKeyHeader(builder);
        builder = addTfaHeader(builder);
        builder = addRequestOriginHeader(builder);
        builder = applyInterceptors(builder);
        Entity<?> request = Entity.entity(responseParser.write(representation), contentType);
        return builder.post(request);
    }

    private <T extends ResourceRepresentation> Response httpPut(String path, MediaType mediaType, T representation) {
        Invocation.Builder builder = client.target(path).request();

        builder = addAcceptHeader(builder, mediaType);
        builder = addApplicationKeyHeader(builder);
        builder = addTfaHeader(builder);
        builder = addRequestOriginHeader(builder);
        builder = applyInterceptors(builder);
        Entity<?> request = Entity.entity(responseParser.write(representation), mediaType);
        return builder.put(request);
    }

    @Override
    public void delete(String path) throws SDKException {
        Builder builder = client.target(path).request();

        builder = addDefaultAcceptHeader(builder);
        builder = addApplicationKeyHeader(builder);
        builder = addTfaHeader(builder);
        builder = addRequestOriginHeader(builder);
        builder = applyInterceptors(builder);
        Response response = builder.delete();
        responseParser.checkStatus(response, NO_CONTENT.getStatusCode());
    }

    public static Client createClient(PlatformParameters platformParameters) {

        ClientConfig config = new ClientConfig();
        config.connectorProvider(new ApacheConnectorProvider());

        if (isProxyRequired(platformParameters)) {
            config.property(ClientProperties.PROXY_URI, "http://" + platformParameters.getProxyHost() + ":" + platformParameters.getProxyPort());
            if (isProxyAuthenticationRequired(platformParameters)) {
                config.property(ClientProperties.PROXY_USERNAME, platformParameters.getProxyUserId());
                config.property(ClientProperties.PROXY_PASSWORD, platformParameters.getProxyPassword());
            }
        }
        config.property(ClientProperties.READ_TIMEOUT, platformParameters.getHttpClientConfig().getHttpReadTimeout());
        config.property(ClientProperties.FOLLOW_REDIRECTS, true);

        registerClasses(config);
        config.register(new CumulocityAuthenticationFilter(platformParameters.getCumulocityCredentials()));
        config.register(new BufferedResponseStreamInterceptor());

        if (platformParameters.isAlwaysCloseConnection()) {
            config.register((ClientRequestFilter) cr -> cr.getHeaders().add("Connection", "close"));
        }
        if (platformParameters.getChunkedEncodingSize() > 0) {
            config.property(ClientProperties.CHUNKED_ENCODING_SIZE, platformParameters.getChunkedEncodingSize());
        }
        config.property(ApacheClientProperties.CONNECTION_MANAGER, createConnectionManager(platformParameters));
        config.property(ApacheClientProperties.REQUEST_CONFIG, createRequestConfig(platformParameters));


        CumulocityHttpClient client = new CumulocityHttpClient(config);
        client.setPlatformParameters(platformParameters);

        return client;
    }

    private static HttpClientConnectionManager createConnectionManager(PlatformParameters platformParameters) {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        ConnectionPoolConfig pool = platformParameters.getHttpClientConfig().getPool();

        if (pool.isEnabled()) {
            connectionManager.setMaxTotal(pool.getMax());
            connectionManager.setDefaultMaxPerRoute(pool.getPerHost());
        } else {
            connectionManager.setValidateAfterInactivity(ConnectionPoolConfig.STALE_CHECK_DISABLED);
        }
        return connectionManager;
    }

    private static RequestConfig createRequestConfig(PlatformParameters platformParameters) {
        ConnectionPoolConfig pool = platformParameters.getHttpClientConfig().getPool();
        if (pool.isEnabled()) {
            return RequestConfig.custom()
                    .setConnectionRequestTimeout(pool.getAwaitTimeout())
                    .build();
        }
        return RequestConfig.DEFAULT;
    }

    private static boolean isProxyAuthenticationRequired(PlatformParameters platformParameters) {
        return hasText(platformParameters.getProxyUserId()) && hasText(platformParameters.getProxyPassword());
    }

    private static boolean isProxyRequired(PlatformParameters platformParameters) {
        return hasText(platformParameters.getProxyHost()) && (platformParameters.getProxyPort() > 0);
    }

    public static Client createURLConnectionClient(final PlatformParameters platformParameters) {

        ClientConfig config = new ClientConfig()
                .property(ClientProperties.FOLLOW_REDIRECTS, true)
                .connectorProvider(new ApacheConnectorProvider());

        if (isProxyRequired(platformParameters)) {
            config.property(ClientProperties.PROXY_URI, "http://" + platformParameters.getProxyHost() + ":" + platformParameters.getProxyPort());
            if (isProxyAuthenticationRequired(platformParameters)) {
                config.property(ClientProperties.PROXY_USERNAME, platformParameters.getProxyUserId());
                config.property(ClientProperties.PROXY_PASSWORD, platformParameters.getProxyPassword());
            }
        }

        registerClasses(config);

        config.register(new CumulocityAuthenticationFilter(platformParameters.getCumulocityCredentials()));
        config.register(new BufferedResponseStreamInterceptor());

        if (isProxyRequired(platformParameters) && isProxyAuthenticationRequired(platformParameters)) {
            config.register(new HTTPBasicProxyAuthenticationFilter(platformParameters.getProxyUserId(), platformParameters
                    .getProxyPassword()));
        }
        return ClientBuilder.newBuilder()
                .withConfig(config)
                .readTimeout(platformParameters.getHttpClientConfig().getHttpReadTimeout(), TimeUnit.MILLISECONDS)
                .build();
    }

    private static void registerClasses(ClientConfig config) {
        config.register(new MultiPartFeature());
        config.register(new CumulocityJSONMessageBodyWriter());
        config.register(new CumulocityJSONMessageBodyReader());
        config.register(new ErrorMessageRepresentationReader());
    }

    private static boolean hasText(String string) {
        if (string == null) {
            return false;
        }
        if (string.trim().length() <= 0) {
            return false;
        }
        return true;
    }

    private Invocation.Builder getResourceBuilder(String path) {
        Invocation.Builder builder = client.target(path).request();
        builder = addApplicationKeyHeader(builder);
        builder = addTfaHeader(builder);
        builder = addRequestOriginHeader(builder);
        builder = applyInterceptors(builder);
        builder = addAcceptHeader(builder, MediaType.APPLICATION_JSON_TYPE);
        builder = addMimeVersion(builder);
        return builder;
    }

}
