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

import com.cumulocity.model.idtype.GId;
import com.cumulocity.rest.representation.BaseResourceRepresentation;
import com.cumulocity.rest.representation.CumulocityMediaType;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.sdk.client.interceptor.HttpClientInterceptor;
import com.google.common.net.HttpHeaders;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.AdditionalAnswers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RestConnectorTest {

    private static final String PATH = "path";

    private CumulocityMediaType mediaType = CumulocityMediaType.ERROR_MESSAGE;

    @Mock
    private WebTarget webResource;

    @Mock
    private Response response;

    @Mock(lenient = true)
    private Invocation.Builder typeBuilder;

    private PlatformParameters clientParameters = spy(new PlatformParameters());

    @Mock(lenient = true)
    private Client client;

    @Mock(lenient = true)
    private ResponseParser parser;

    private RestConnector restConnector;

    @BeforeEach
    public void setup() {
        restConnector = new RestConnector(clientParameters, parser, client);

        when(client.target(PATH)).thenReturn(webResource);

        when(parser.write(any())).thenAnswer(AdditionalAnswers.returnsFirstArg());
    }

    @Test
    public void shouldRetrieveResource() {
        // Given
        Response response = Response.accepted().build();
        when(webResource.request()).thenReturn(typeBuilder);
        when(typeBuilder.get()).thenReturn(response);
        returnResponseWhenSending();

        BaseResourceRepresentation representation = new BaseResourceRepresentation();
        when(parser.parse(response, BaseResourceRepresentation.class, 200)).thenReturn(representation);

        // When
        BaseResourceRepresentation result = restConnector.get(PATH, mediaType,
                BaseResourceRepresentation.class);

        // Then
        assertThat(result, sameInstance(representation));
    }

    @Test
    public void shouldPostBaseRepresentation() {
        // Given
        BaseResourceRepresentation representation = new BaseResourceRepresentation();
        returnResponseWhenPosting(representation);
        BaseResourceRepresentation outputRepresentation = new BaseResourceRepresentation();
        when(parser.parse(response, BaseResourceRepresentation.class, 201, 200)).thenReturn(outputRepresentation);

        // When
        BaseResourceRepresentation result = restConnector.post(PATH, mediaType, representation);

        // Then
        assertThat(result, sameInstance(outputRepresentation));
    }

    @Test
    public void shouldPostRepresentationSupportingId() {
        // Given
        ManagedObjectRepresentation representation = new ManagedObjectRepresentation();
        returnResponseWhenPosting(representation);
        ManagedObjectRepresentation outputRepresentation = new ManagedObjectRepresentation();
        when(parser.parse(response, ManagedObjectRepresentation.class, 201)).thenReturn(outputRepresentation);

        // When
        ManagedObjectRepresentation result = restConnector.post(PATH, mediaType, representation);

        // Then
        assertThat(result, sameInstance(outputRepresentation));
    }

    @Test
    public void shouldPostRepresentationSupportingIdWithNoResponseBody() throws URISyntaxException {
        // Given
        clientParameters.setRequireResponseBody(false);

        ManagedObjectRepresentation representation = new ManagedObjectRepresentation();
        returnResponseWhenPosting(representation);
        when(parser.parse(response, ManagedObjectRepresentation.class, 201)).thenReturn(null);
        when(response.getLocation()).thenReturn(new URI("http://URI"));
        when(parser.parseIdFromLocation(response)).thenReturn(new GId("mo_id"));

        // When
        ManagedObjectRepresentation result = restConnector.post(PATH, mediaType, representation);

        // Then
        verify(typeBuilder, never()).accept(any(MediaType.class));
        assertThat(result, sameInstance(representation));
        assertThat(result.getId(), is(new GId("mo_id")));
    }

    @Test
    public void shouldPostRepresentationSupportingIdWithNoResponseBodyAndNoLocation() {
        // Given
        clientParameters.setRequireResponseBody(false);

        ManagedObjectRepresentation representation = new ManagedObjectRepresentation();
        returnResponseWhenPosting(representation);

        when(parser.parse(response, ManagedObjectRepresentation.class, 201)).thenReturn(null);
        when(response.getLocation()).thenReturn(null);

        // When
        ManagedObjectRepresentation result = restConnector.post(PATH, mediaType, representation);

        // Then
        verify(typeBuilder, never()).accept(any(MediaType.class));
        assertThat(result, sameInstance(representation));
        assertThat(result.getId(), nullValue());
    }


    private void returnResponseWhenPosting(BaseResourceRepresentation representation) {
        returnResponseWhenSending();
        when(typeBuilder.post(Entity.entity(representation, mediaType))).thenReturn(response);
    }

    private void returnResponseWhenSending() {
        when(webResource.request()).thenReturn(typeBuilder);
        when(typeBuilder.accept(mediaType)).thenReturn(typeBuilder);
        acceptDefaultMediaType();
    }

    private void acceptDefaultMediaType() {
        when(typeBuilder.accept(CumulocityMediaType.WILDCARD)).thenReturn(typeBuilder);
        when(typeBuilder.header(eq(HttpHeaders.ACCEPT), any())).thenReturn(typeBuilder);
    }

    @Test
    public void shouldDelete() {
        // Given
        when(webResource.request()).thenReturn(typeBuilder);
        when(typeBuilder.delete()).thenReturn(response);
        acceptDefaultMediaType();

        // When
        restConnector.delete(PATH);

        // Then
        verify(parser).checkStatus(response, 204);
    }

    @Test
    public void shouldPutBaseRepresentation() {
        // Given
        BaseResourceRepresentation representation = new BaseResourceRepresentation();
        BaseResourceRepresentation outputRepresentation = new BaseResourceRepresentation();
        returnResponseWhenPut(representation);
        when(parser.parse(response, BaseResourceRepresentation.class, 200)).thenReturn(outputRepresentation);

        // When
        BaseResourceRepresentation result = restConnector.put(PATH, mediaType, representation);

        // Then
        assertThat(result, sameInstance(outputRepresentation));
    }

    private void returnResponseWhenPut(BaseResourceRepresentation representation) {
        returnResponseWhenSending();
        when(typeBuilder.put(Entity.entity(representation, mediaType))).thenReturn(response);
    }

    @Test
    public void shouldPutRepresentationSupportingId() {
        // Given
        ManagedObjectRepresentation representation = new ManagedObjectRepresentation();
        returnResponseWhenPut(representation);
        ManagedObjectRepresentation outputRepresentation = new ManagedObjectRepresentation();
        when(parser.parse(response, ManagedObjectRepresentation.class, 200)).thenReturn(outputRepresentation);

        // When
        ManagedObjectRepresentation result = restConnector.put(PATH, mediaType, representation);

        // Then
        assertThat(result, sameInstance(outputRepresentation));
    }

    @Test
    public void shouldPutRepresentationSupportingIdWithNoResponseBody() throws Exception {
        // Given
        clientParameters.setRequireResponseBody(false);

        ManagedObjectRepresentation representation = new ManagedObjectRepresentation();
        returnResponseWhenPut(representation);
        when(parser.parse(response, ManagedObjectRepresentation.class, 200)).thenReturn(null);
        when(response.getLocation()).thenReturn(new URI("http://URI"));
        when(parser.parseIdFromLocation(response)).thenReturn(new GId("mo_id"));

        // When
        ManagedObjectRepresentation result = restConnector.put(PATH, mediaType, representation);

        // Then
        verify(typeBuilder, never()).accept(any(MediaType.class));
        assertThat(result, sameInstance(representation));
        assertThat(result.getId(), is(new GId("mo_id")));
    }

    @Test
    public void shouldPutRepresentationSupportingIdWithNoResponseBodyAndNoLocation() {
        // Given
        clientParameters.setRequireResponseBody(false);

        ManagedObjectRepresentation representation = new ManagedObjectRepresentation();
        returnResponseWhenPut(representation);
        when(parser.parse(response, ManagedObjectRepresentation.class, 200)).thenReturn(null);
        when(response.getLocation()).thenReturn(null);

        // When
        ManagedObjectRepresentation result = restConnector.put(PATH, mediaType, representation);

        // Then
        verify(typeBuilder, never()).accept(any(MediaType.class));
        assertThat(result, sameInstance(representation));
        assertThat(result.getId(), nullValue());
    }

    @Test
    public void shouldAddHeaderFormInterceptor() {
        // Given
        clientParameters.registerInterceptor(new HttpClientInterceptor() {
            @Override
            public Invocation.Builder apply(Invocation.Builder builder) {
                builder.header("fake", "fake");
                return builder;
            }
        });

        BaseResourceRepresentation representation = new BaseResourceRepresentation();
        returnResponseWhenPosting(representation);
        BaseResourceRepresentation outputRepresentation = new BaseResourceRepresentation();
        when(parser.parse(response, BaseResourceRepresentation.class, 201, 200)).thenReturn(outputRepresentation);

        // When
        BaseResourceRepresentation result = restConnector.post(PATH, mediaType, representation);

        // Then
        verify(typeBuilder).header(eq("fake"), eq("fake"));
        assertThat(result, sameInstance(outputRepresentation));
    }

    @Test
    public void shouldNotAddHeaderFormInterceptorAfterRemovingInterceptor() {
        // given
        HttpClientInterceptor addHeaderFake = new HttpClientInterceptor() {
            @Override
            public Invocation.Builder apply(Invocation.Builder builder) {
                builder.header("fake", "fake");
                return builder;
            }
        };

        BaseResourceRepresentation representation = new BaseResourceRepresentation();
        returnResponseWhenPosting(representation);
        BaseResourceRepresentation outputRepresentation = new BaseResourceRepresentation();
        when(parser.parse(response, BaseResourceRepresentation.class, 201)).thenReturn(outputRepresentation);

        { // recheck if work ok on register interceptor
            clientParameters.registerInterceptor(addHeaderFake);
            restConnector.post(PATH, mediaType, representation);
            verify(typeBuilder).header(eq("fake"), eq("fake"));
            reset(typeBuilder);
            returnResponseWhenPosting(representation);
        }

        clientParameters.unregisterInterceptor(addHeaderFake);

        // when
        restConnector.post(PATH, mediaType, representation);

        // then
        verify(typeBuilder, never()).header(eq("fake"), eq("fake"));
    }

    @Test
    public void shouldUseDefaultReadTimeoutWhenNotSet() {

        //when
        Client client = RestConnector.createURLConnectionClient(clientParameters);

        //then
        Integer readTimeout = (Integer) client.getConfiguration().getProperty(ClientProperties.READ_TIMEOUT);
        assertThat("Should be default 180000", readTimeout == 180000);
    }

    @Test
    public void shouldUseCustomHttpReadTimeoutWhenSpecified() {
        // given
        HttpClientConfig httpClientConfig = HttpClientConfig.httpConfig().httpReadTimeout(360000).build();
        clientParameters.setHttpClientConfig(httpClientConfig);

        // when
        Client client = RestConnector.createURLConnectionClient(clientParameters);

        //then
        Integer readTimeout = (Integer) client.getConfiguration().getProperty(ClientProperties.READ_TIMEOUT);
        assertThat("Should be default 360000", readTimeout == 360000);
    }

    @ParameterizedTest
    @CsvSource(value = {",0", "'',0", "'  ',0", "'app-key',1"})
    public void shouldIncludeApplicationKeyHeader(String applicationKey, int invocations) {
        // Given
        ManagedObjectRepresentation representation = new ManagedObjectRepresentation();
        returnResponseWhenPosting(representation);
        ManagedObjectRepresentation outputRepresentation = new ManagedObjectRepresentation();
        when(parser.parse(response, ManagedObjectRepresentation.class, 201)).thenReturn(outputRepresentation);
        when(clientParameters.getApplicationKey()).thenReturn(applicationKey);
        when(typeBuilder.header(any(), any())).thenReturn(typeBuilder);

        // When
        restConnector.post(PATH, mediaType, representation);

        // Then
        verify(typeBuilder, times(invocations)).header(eq(RestConnector.X_CUMULOCITY_APPLICATION_KEY), invocations > 0 ? eq(applicationKey) : any());
    }

    @ParameterizedTest
    @CsvSource(value = {",0", "'',0", "'  ',0", "'request-origin',1"})
    public void shouldIncludeTfaTokenHeader(String tfaToken, int invocations) {
        // Given
        ManagedObjectRepresentation representation = new ManagedObjectRepresentation();
        returnResponseWhenPosting(representation);
        ManagedObjectRepresentation outputRepresentation = new ManagedObjectRepresentation();
        when(parser.parse(response, ManagedObjectRepresentation.class, 201)).thenReturn(outputRepresentation);
        when(clientParameters.getTfaToken()).thenReturn(tfaToken);
        when(typeBuilder.header(any(), any())).thenReturn(typeBuilder);

        // When
        restConnector.post(PATH, mediaType, representation);

        // Then
        verify(typeBuilder, times(invocations)).header(eq(RestConnector.TFA_TOKEN_HEADER), invocations > 0 ? eq(tfaToken) : any());
    }

    @ParameterizedTest
    @CsvSource(value = {",0", "'',0", "'  ',0", "'request-origin',1"})
    public void shouldIncludeRequestOriginHeader(String requestOrigin, int invocations) {
        // Given
        ManagedObjectRepresentation representation = new ManagedObjectRepresentation();
        returnResponseWhenPosting(representation);
        ManagedObjectRepresentation outputRepresentation = new ManagedObjectRepresentation();
        when(parser.parse(response, ManagedObjectRepresentation.class, 201)).thenReturn(outputRepresentation);
        when(clientParameters.getRequestOrigin()).thenReturn(requestOrigin);
        when(typeBuilder.header(any(), any())).thenReturn(typeBuilder);

        // When
        restConnector.post(PATH, mediaType, representation);

        // Then
        verify(typeBuilder, times(invocations)).header(eq(RestConnector.X_CUMULOCITY_REQUEST_ORIGIN), invocations > 0 ? eq(requestOrigin) : any());
    }

    @Test
    void shouldSetFileTypeWhenPostingFileStream() {
        //given
        MediaType mediaType = MediaType.TEXT_PLAIN_TYPE;
        ByteArrayInputStream inputStream = new ByteArrayInputStream("payload".getBytes(StandardCharsets.UTF_8));
        when(webResource.request()).thenReturn(typeBuilder);
        when(typeBuilder.accept(MediaType.APPLICATION_JSON_TYPE)).thenReturn(typeBuilder);

        //when
        restConnector.postStream(PATH, inputStream, mediaType);

        //then
        ArgumentCaptor<Entity> captor = ArgumentCaptor.forClass(Entity.class);
        verify(typeBuilder).post(captor.capture());
        Assertions.assertThat(captor.getValue().getEntity())
                .asInstanceOf(InstanceOfAssertFactories.type(FormDataMultiPart.class))
                .extracting(form -> form.getField("file").getMediaType())
                .isEqualTo(mediaType);
    }
}
