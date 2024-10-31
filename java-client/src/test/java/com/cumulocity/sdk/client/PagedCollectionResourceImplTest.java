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
import com.cumulocity.rest.representation.PageStatisticsRepresentation;
import com.cumulocity.rest.representation.TestCollectionRepresentation;
import com.cumulocity.rest.representation.measurement.MeasurementCollectionRepresentation;
import com.cumulocity.sdk.client.common.UriMatcher;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

public class PagedCollectionResourceImplTest {

    private static final Class<TestCollectionRepresentation<Object>> CLAZZ =
            (Class<TestCollectionRepresentation<Object>>) new TestCollectionRepresentation<Object>().getClass();

    private static final CumulocityMediaType MEDIA_TYPE = CumulocityMediaType.ERROR_MESSAGE;

    private static final String URL = "http://hello.com/blah/blah";

    private static final int PAGE_SIZE = 333;


    PagedCollectionResource<Object, TestCollectionRepresentation<Object>> target;

    @Mock
    Response clientResponse;

    @Mock
    private RestConnector restConnector;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);

        target = createPagedCollectionResource(restConnector, URL, PAGE_SIZE, MEDIA_TYPE, CLAZZ);

        MeasurementCollectionRepresentation measurementCollectionRepresentation = new MeasurementCollectionRepresentation();
        when(clientResponse.getStatus()).thenReturn(200);
        when(clientResponse.readEntity(MeasurementCollectionRepresentation.class)).thenReturn(measurementCollectionRepresentation);
    }

    @Test
    public void shouldGetAndAddPageSizeParamWhenNoOtherParams() throws SDKException {
        // Given
        TestCollectionRepresentation expectedRepresentation = new TestCollectionRepresentation();
        when(restConnector.get(URL + "?pageSize=" + PAGE_SIZE, MEDIA_TYPE, CLAZZ)).thenReturn(expectedRepresentation);

        // When
        TestCollectionRepresentation page = target.get();

        // Then
        assertThat(page, sameInstance(expectedRepresentation));
    }

    @Test
    public void shouldGetAndAddPageSizeParamWhenOtherParamsPresent() throws SDKException {
        // Given
        String myUrlWithOtherParam = URL + "?param=value";
        target = createPagedCollectionResource(restConnector, myUrlWithOtherParam, PAGE_SIZE, MEDIA_TYPE, CLAZZ);
        TestCollectionRepresentation expectedRepresentation = new TestCollectionRepresentation();
        String expectedUrl = myUrlWithOtherParam + "&pageSize=" + PAGE_SIZE;

        when(restConnector.get(eq(expectedUrl), eq(MEDIA_TYPE), eq(CLAZZ))).thenReturn(expectedRepresentation);

        // When
        TestCollectionRepresentation page = target.get();

        // Then
        assertThat(page, sameInstance(expectedRepresentation));
    }

    @Test
    public void shouldGetAndReplaceAlreadyPresentPageSizeParam() throws SDKException {
        // Given
        String myUrlWithOtherParamAndPageSize = URL + "?param1=value1&pageSize=15&param2=value2";
        target = createPagedCollectionResource(restConnector, myUrlWithOtherParamAndPageSize, PAGE_SIZE, MEDIA_TYPE, CLAZZ);
        TestCollectionRepresentation expectedRepresentation = new TestCollectionRepresentation();
        String expectedUrl = URL + "?param1=value1&param2=value2&pageSize=" + PAGE_SIZE;

        when(restConnector.get(argThat(new UriMatcher(expectedUrl)), eq(MEDIA_TYPE), eq(CLAZZ))).thenReturn(expectedRepresentation);

        // When
        TestCollectionRepresentation page = target.get();

        // Then
        assertThat(page, sameInstance(expectedRepresentation));
    }

    @Test
    public void shouldGetPageWhenNoParamsPresent() throws SDKException {
        // Given
        TestCollectionRepresentation input = new TestCollectionRepresentation();
        input.setPageStatistics(new PageStatisticsRepresentation(PAGE_SIZE));
        input.setSelf(URL + "/measuremnt/measurments");

        String expectedUrl = input.getSelf() + "?pageSize=" + PAGE_SIZE + "&currentPage=5";
        TestCollectionRepresentation expectedRep = new TestCollectionRepresentation();
        when(restConnector.get(argThat(new UriMatcher(expectedUrl)), eq(MEDIA_TYPE), eq(CLAZZ))).thenReturn(expectedRep);

        // When
        TestCollectionRepresentation page = target.getPage(input, 5);

        // Then
        assertThat(page, sameInstance(expectedRep));
    }

    @Test
    public void shouldGetPageWhenWithGivenPageSize() throws SDKException {
        // Given
        TestCollectionRepresentation input = new TestCollectionRepresentation();
        String myUrl = URL + "/measuremnt/measurments";
        input.setSelf(myUrl);

        String expectedUrl = myUrl + "?pageSize=50&currentPage=5";
        TestCollectionRepresentation expectedRep = new TestCollectionRepresentation();
        when(restConnector.get(argThat(new UriMatcher(expectedUrl)), eq(MEDIA_TYPE), eq(CLAZZ))).thenReturn(expectedRep);

        // When
        TestCollectionRepresentation page = target.getPage(input, 5, 50);

        // Then
        assertThat(page, sameInstance(expectedRep));
    }

    @Test
    public void shouldGetPageWhenOtherParamsAlreadyPresent() throws SDKException {
        // Given
        TestCollectionRepresentation input = new TestCollectionRepresentation();
        input.setPageStatistics(new PageStatisticsRepresentation(PAGE_SIZE));
        input.setSelf(URL + "/measuremnt/measurments?param1=value1");

        String expectedUrl = input.getSelf() + "&pageSize=" + PAGE_SIZE + "&currentPage=5";
        TestCollectionRepresentation expectedRep = new TestCollectionRepresentation();
        when(restConnector.get(argThat(new UriMatcher(expectedUrl)), eq(MEDIA_TYPE), eq(CLAZZ))).thenReturn(expectedRep);

        // When
        TestCollectionRepresentation page = target.getPage(input, 5);

        // Then
        assertThat(page, sameInstance(expectedRep));
    }

    @Test
    public void shouldGetPageAndReplaceAlreadyPresentPageSizeAndCurrentPageParams() throws SDKException {
        // Given
        TestCollectionRepresentation input = new TestCollectionRepresentation();
        input.setPageStatistics(new PageStatisticsRepresentation(PAGE_SIZE));
        String myUrl = URL + "/measuremnt/measurments?param1=value1&pageSize=123&currentPage=234";
        input.setSelf(myUrl);

        String expectedUrl = URL + "/measuremnt/measurments?param1=value1" + "&pageSize=" + PAGE_SIZE + "&currentPage=6";
        TestCollectionRepresentation expectedRep = new TestCollectionRepresentation();
        when(restConnector.get(argThat(new UriMatcher(expectedUrl)), eq(MEDIA_TYPE), eq(CLAZZ))).thenReturn(expectedRep);

        // When
        TestCollectionRepresentation page = target.getPage(input, 6);

        // Then
        assertThat(page, sameInstance(expectedRep));
    }

    @Test
    public void shouldGetPageValidInput() throws SDKException {
        // Given
        TestCollectionRepresentation input = null;

        // When
        Throwable thrown = catchThrowable(() -> target.getPage(input, 5));

        // Then
        assertThat(thrown, is(instanceOf(SDKException.class)));
    }

    @Test
    public void shouldGetNextPage() throws SDKException {
        //Given
        String url = "/measuremnt/measurments";
        TestCollectionRepresentation input = new TestCollectionRepresentation();
        input.setNext(url);
        TestCollectionRepresentation expectedResult = new TestCollectionRepresentation();

        when(restConnector.get(url, MEDIA_TYPE, CLAZZ)).thenReturn(expectedResult);

        //when
        TestCollectionRepresentation nextPage = target.getNextPage(input);

        // Then
        assertThat(nextPage, is(expectedResult));
    }

    @Test
    public void shouldGetNextPageInvalidInput() throws SDKException {
        // Given
        TestCollectionRepresentation input = new TestCollectionRepresentation();

        // When
        TestCollectionRepresentation nextPage = target.getNextPage(input);

        // Then
        assertThat(nextPage, is(nullValue()));
    }

    @Test
    public void shouldGetPreviousPage() throws SDKException {
        // Given
        String url = "/measuremnt/measurments";
        TestCollectionRepresentation input = new TestCollectionRepresentation();
        input.setPrev(url);

        TestCollectionRepresentation expectedResult = new TestCollectionRepresentation();
        when(restConnector.get(url, MEDIA_TYPE, CLAZZ)).thenReturn(expectedResult);

        // When
        TestCollectionRepresentation prevPage = target.getPreviousPage(input);

        // Then
        assertThat(prevPage, is(expectedResult));
    }

    @Test
    public void shouldGetPreviousPageInvalidInput() throws SDKException {
        // Given
        TestCollectionRepresentation input = new TestCollectionRepresentation();

        // When
        TestCollectionRepresentation prevPage = target.getPreviousPage(input);

        // Then
        assertThat(prevPage, is(nullValue()));
    }

    @Test
    public void shouldEqualsIfPageSizeAndUrlAndMediaTypeAndRepresentationClassEqual() throws Exception {
        // Given
        PagedCollectionResource<Object, TestCollectionRepresentation<Object>> equalObject = createPagedCollectionResource(
                restConnector, URL, PAGE_SIZE, MEDIA_TYPE, CLAZZ);

        //When
        boolean equals = target.equals(equalObject);

        //Then
        assertThat(equals, is(true));
    }

    @Test
    public void shouldNotEqualsIfAnyOfParamsIsDifferent() throws Exception {
        // Given
        String differentUrl = "differentUrl";
        PagedCollectionResource<Object, TestCollectionRepresentation<Object>> equalObject = createPagedCollectionResource(restConnector, differentUrl,
                PAGE_SIZE, MEDIA_TYPE, CLAZZ);

        //When
        boolean equals = target.equals(equalObject);

        //Then
        assertThat(equals, is(false));
    }

    @Test
    public void shouldNotEqualsIfDifferentClass() throws Exception {
        //When
        boolean equals = target.equals(new Object());

        //Then
        assertThat(equals, is(false));
    }

    @Test
    public void shouldNotEqualsIfAnyOfFieldsIsNull() throws Exception {
        target = createPagedCollectionResource(restConnector, null, PAGE_SIZE, MEDIA_TYPE, CLAZZ);

        //When
        boolean equals = target.equals(target);

        //Then
        assertThat(equals, is(false));
    }

    @Test
    public void shouldHaveProperHashCode() throws Exception {
        // Given
        int expectedHashCode = MEDIA_TYPE.hashCode() ^ CLAZZ.hashCode() ^ PAGE_SIZE ^ URL.hashCode();

        //When
        int hashCode = target.hashCode();

        //Then
        assertThat(hashCode, is(expectedHashCode));
    }

    private PagedCollectionResource<Object, TestCollectionRepresentation<Object>> createPagedCollectionResource(RestConnector restConnector,
            String url, int pageSize, final CumulocityMediaType mediaType, final Class<TestCollectionRepresentation<Object>> clazz) {
        return new PagedCollectionResourceImpl<Object, TestCollectionRepresentation<Object>, TestCollectionRepresentation<Object>>(restConnector, url, pageSize) {
            @Override
            protected CumulocityMediaType getMediaType() {
                return mediaType;
            }

            @Override
            protected Class<TestCollectionRepresentation<Object>> getResponseClass() {
                return clazz;
            }

            @Override
            protected TestCollectionRepresentation wrap(TestCollectionRepresentation collection) {
                return collection;
            }
        };
    }
}
