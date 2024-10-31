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
import com.cumulocity.rest.representation.ErrorMessageRepresentation;
import com.cumulocity.rest.representation.inventory.InventoryRepresentation;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import java.net.URI;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.when;

public class ResponseParserTest {

    private static final int EXPECTED_STATUS = 200;

    private static final int ERROR_STATUS = 500;

    @Mock
    private Response response;

    private ResponseParser parser;

    @BeforeEach
    public void before() {
        MockitoAnnotations.initMocks(this);

        parser = new ResponseParser();
    }

    @Test
    public void shouldParse() {
        // Given
        when(response.getStatus()).thenReturn(EXPECTED_STATUS);
        BaseResourceRepresentation representation = new BaseResourceRepresentation();
        when(response.readEntity(BaseResourceRepresentation.class)).thenReturn(representation);

        // When
        BaseResourceRepresentation result = parser.parse(response,
                BaseResourceRepresentation.class, EXPECTED_STATUS);

        // Then
        assertThat(result, sameInstance(representation));
    }

    @Test
    public void shouldParseDifferentSpecificType() throws Exception {
        // Given
        when(response.getStatus()).thenReturn(EXPECTED_STATUS);
        InventoryRepresentation representation = new InventoryRepresentation();
        when(response.readEntity(InventoryRepresentation.class)).thenReturn(representation);

        // When
        InventoryRepresentation result = parser.parse(response, InventoryRepresentation.class, EXPECTED_STATUS);

        // Then
        assertThat(result, sameInstance(representation));
    }

    @Test
    public void shouldIncludeExistingErrorMessageInExceptionWhenStatusIsNotAsExpected() throws Exception {
        // Given
        when(response.getStatus()).thenReturn(ERROR_STATUS);
        ErrorMessageRepresentation errorRepresentation = new ErrorMessageRepresentation();
        when(response.hasEntity()).thenReturn(true);
        when(response.getMediaType()).thenReturn(MediaType.APPLICATION_JSON_TYPE);
        when(response.readEntity(ErrorMessageRepresentation.class)).thenReturn(errorRepresentation);

        // When
        Throwable thrown = catchThrowable(() -> parser.parse(response, BaseResourceRepresentation.class, EXPECTED_STATUS));

        // Then
        Assertions.assertThat(thrown).isInstanceOf(SDKException.class)
                .hasMessage("Http status code: " + ERROR_STATUS + "\n" + errorRepresentation);
    }

    @Test
    public void shouldIncludeSpecificResponseHeaderInExceptionWhenStatusIsNotAsExpected() {
        // Given
        when(response.getStatus()).thenReturn(ERROR_STATUS);
        ErrorMessageRepresentation errorRepresentation = new ErrorMessageRepresentation();
        when(response.hasEntity()).thenReturn(true);
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add("Content-Type", "application/json");
        headers.add("Location", "en.wikipedia.org:8080");
        headers.add("x-authorization", "1bdc5db0-dfc8-43f2-9159-3827bfe48ea7.1587315996051");
        when(response.getStringHeaders()).thenReturn(headers);
        when(response.readEntity(ErrorMessageRepresentation.class)).thenReturn(errorRepresentation);

        // When
        Throwable thrown = catchThrowable(() -> parser.parse(response, BaseResourceRepresentation.class, EXPECTED_STATUS));

        // Then
        Assertions.assertThat(thrown).isInstanceOf(SDKException.class)
                .hasMessageContaining("Location: [en.wikipedia.org:8080]");
        Assertions.assertThat(thrown).isInstanceOf(SDKException.class)
                .hasMessageNotContaining("Content-Type: [application/json]");
        Assertions.assertThat(thrown).isInstanceOf(SDKException.class)
                .hasMessageNotContaining("x-authorization");
    }

    @Test
    public void shouldNotIncludeErrorMessageIfStatusIsNotAsExpected() throws Exception {
        // Given
        when(response.getStatus()).thenReturn(ERROR_STATUS);
        when(response.hasEntity()).thenReturn(false);
        when(response.readEntity(ErrorMessageRepresentation.class)).thenThrow(new RuntimeException());
        when(response.getMediaType()).thenReturn(MediaType.APPLICATION_JSON_TYPE);

        // When
        Throwable thrown = catchThrowable(() -> parser.parse(response, BaseResourceRepresentation.class, EXPECTED_STATUS));

        // Then
        Assertions.assertThat(thrown).isInstanceOf(SDKException.class)
                .hasMessage("Http status code: %s", ERROR_STATUS);
    }

    @Test
    public void shouldParseIdFromHeader() throws Exception {
        // Given
        when(response.getLocation()).thenReturn(
                new URI("http://integration.cumulocity.com/inventory/managedObjects/10200"));

        // When
        GId gid = parser.parseIdFromLocation(response);

        // Then
        assertThat(gid, is(new GId("10200")));
    }
}
