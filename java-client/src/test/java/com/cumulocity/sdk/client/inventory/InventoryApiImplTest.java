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
package com.cumulocity.sdk.client.inventory;

import com.cumulocity.model.idtype.GId;
import com.cumulocity.rest.representation.inventory.*;
import com.cumulocity.sdk.client.RestConnector;
import com.cumulocity.sdk.client.SDKException;
import com.cumulocity.sdk.client.UrlProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import jakarta.ws.rs.core.MediaType;
import java.util.ArrayList;

import static com.cumulocity.rest.representation.inventory.InventoryMediaType.MANAGED_OBJECT;
import static com.cumulocity.rest.representation.inventory.InventoryMediaType.MANAGED_OBJECT_COLLECTION;
import static com.cumulocity.sdk.client.inventory.InventoryFilter.searchInventory;
import static com.cumulocity.sdk.client.inventory.InventoryParam.withoutChildren;
import static com.cumulocity.sdk.client.inventory.InventoryParam.withoutParents;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class InventoryApiImplTest {

    private static final String TEMPLATE_URL = "template_url";

    private static final String INVENTORY_COLLECTION_URL = "inventory_collection_url";

    private static final int DEFAULT_PAGE_SIZE = 9;

    InventoryApi inventoryApiResource;

    InventoryRepresentation inventoryRepresentation = new InventoryRepresentation();

    @Mock
    private RestConnector restConnector;

    private UrlProcessor urlProcessor = new UrlProcessor();

    @BeforeEach
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);

        ManagedObjectReferenceCollectionRepresentation representation = new ManagedObjectReferenceCollectionRepresentation();
        representation.setSelf(INVENTORY_COLLECTION_URL);
        inventoryRepresentation.setManagedObjects(representation);

        inventoryApiResource = new InventoryApiImpl(restConnector, urlProcessor, inventoryRepresentation, DEFAULT_PAGE_SIZE);
    }

    @Test
    public void shouldCreateMo() {
        // Given
        ManagedObjectRepresentation managedObjectRepresentation = new ManagedObjectRepresentation();
        ManagedObjectRepresentation created = new ManagedObjectRepresentation();
        when(restConnector.post(INVENTORY_COLLECTION_URL, InventoryMediaType.MANAGED_OBJECT, managedObjectRepresentation)).thenReturn(
                created);

        // When
        ManagedObjectRepresentation mor = inventoryApiResource.create(managedObjectRepresentation);

        // Then
        assertThat(mor, sameInstance(created));
    }

    @Test
    public void shouldGetMo() {
        // Given
        String gidValue = "gid_value";
        GId gid = new GId(gidValue);

        //When
        ManagedObjectImpl mo = (ManagedObjectImpl) inventoryApiResource.getManagedObject(gid);

        // Then
        assertThat(mo.url, is(INVENTORY_COLLECTION_URL + "/" + gidValue));
    }

    @Test
    public void shouldThrowExceptionWhenGetWithNullInput() throws SDKException {
        // When
        Throwable thrown = catchThrowable(() -> inventoryApiResource.getManagedObject(null));

        // Then
        assertThat(thrown, is(instanceOf(SDKException.class)));
    }

    @Test
    public void shouldThrowExceptionWhenGetWithIncorrectGid() throws SDKException {
        // Given
        GId gid = new GId();

        // When
        Throwable thrown = catchThrowable(() -> inventoryApiResource.getManagedObject(gid));

        // Then
        assertThat(thrown, is(instanceOf(SDKException.class)));
    }

    @Test
    public void shouldGetMos() throws SDKException {
        // Given
        ManagedObjectCollection expected = new ManagedObjectCollectionImpl(restConnector,
                INVENTORY_COLLECTION_URL, DEFAULT_PAGE_SIZE);

        // When
        ManagedObjectCollection result = inventoryApiResource.getManagedObjects();

        // Then
        assertThat(result, is(expected));
    }

    @Test
    public void shouldGetMosByEmptyFilter() throws SDKException {
        // Given
        inventoryRepresentation.setManagedObjectsForType(TEMPLATE_URL);
        ManagedObjectCollection expected = new ManagedObjectCollectionImpl(restConnector,
                INVENTORY_COLLECTION_URL, DEFAULT_PAGE_SIZE);

        // When
        InventoryFilter filter = new InventoryFilter();
        ManagedObjectCollection result = inventoryApiResource.getManagedObjectsByFilter(filter);

        // Then
        assertThat(result, is(expected));
    }

    @Test
    public void shouldGetMosByTypeFilter() throws SDKException {
        // Given
        String myType = "myType";
        InventoryFilter filter = new InventoryFilter().byType(myType);
        inventoryRepresentation.setManagedObjectsForType(TEMPLATE_URL);
        String moByTypeUrl = INVENTORY_COLLECTION_URL + "?type=" + myType;
        ManagedObjectCollection expected = new ManagedObjectCollectionImpl(restConnector, moByTypeUrl, DEFAULT_PAGE_SIZE);

        // When
        ManagedObjectCollection result = inventoryApiResource.getManagedObjectsByFilter(filter);

        // Then
        assertThat(result, is(expected));
    }

    @Test
    public void shouldAddQueryParamWithoutChildrenWhenFetchingManagedObject() {
        // Given
        GId id = GId.asGId(1l);
        givenRespondManagedObject(id);
        // When;
        ManagedObjectRepresentation managedObject = inventoryApiResource.get(id, withoutChildren());
        // Then

        verify(restConnector).get(contains("withChildren=false"), eq(MANAGED_OBJECT), eq(ManagedObjectRepresentation.class));
        assertThat(managedObject, notNullValue());
    }

    @Test
    public void shouldAddQueryParamsWhenFetchingManagedObject() {
        // Given
        GId id = GId.asGId(1l);
        givenRespondManagedObject(id);
        // When;
        ManagedObjectRepresentation managedObject = inventoryApiResource.get(id, withoutChildren(), withoutParents());
        // Then
        verify(restConnector).get(argThat(url -> url.contains("withChildren=false") && url.contains("withParents=false")), eq(MANAGED_OBJECT), eq(ManagedObjectRepresentation.class));
        assertThat(managedObject, notNullValue());
    }

    @Test
    public void shouldAddQueryParamsWhenFetchingManagedObjectCollection() {
        // Given
        InventoryFilter filter = searchInventory();
        givenRespondWithEmptyListOnManagedObjectCollectionQuery();
        // When
        PagedManagedObjectCollectionRepresentation list = inventoryApiResource.getManagedObjectsByFilter(filter).get(withoutChildren());
        // Then
        verify(restConnector).get(contains("withChildren=false"), eq(MANAGED_OBJECT_COLLECTION), eq(ManagedObjectCollectionRepresentation.class));
        assertThat(list, notNullValue());

    }

    @Test
    public void shouldCountMosByEmptyFilter() throws SDKException {
        //Given
        String expectedUrl = INVENTORY_COLLECTION_URL + "/count";
        InventoryFilter filter = new InventoryFilter();

        // When
        inventoryApiResource.countManagedObjectsByFilter(filter);

        // Then
        verify(restConnector).get(expectedUrl, MediaType.APPLICATION_JSON_TYPE, Integer.class);
    }

    @Test
    public void shouldCountMosByTypeFilter() throws SDKException {
        //Given
        String myType = "myType";
        InventoryFilter filter = new InventoryFilter().byType(myType);
        String expectedUrl = INVENTORY_COLLECTION_URL + "/count?type=myType";

        // When
        inventoryApiResource.countManagedObjectsByFilter(filter);

        // Then
        verify(restConnector).get(expectedUrl, MediaType.APPLICATION_JSON_TYPE, Integer.class);
    }

    @Test
    public void shouldThrowExceptionWhenCountByFilterWithNullInput() throws SDKException {
        // When
        Throwable thrown = catchThrowable(() -> inventoryApiResource.countManagedObjectsByFilter(null));

        // Then
        assertThat(thrown, is(instanceOf(IllegalArgumentException.class)));
    }

    private void givenRespondManagedObject(GId id) {
        ManagedObjectRepresentation mo = new ManagedObjectRepresentation();
        when(restConnector.get(contains(INVENTORY_COLLECTION_URL+"/"+ GId.asString(id)), eq(MANAGED_OBJECT), eq(ManagedObjectRepresentation.class)))
                .thenReturn(mo);
    }

    private void givenRespondWithEmptyListOnManagedObjectCollectionQuery() {
        ManagedObjectCollectionRepresentation collection = new ManagedObjectCollectionRepresentation();
        collection.setManagedObjects(new ArrayList<>());
        when(restConnector.get(contains(INVENTORY_COLLECTION_URL), eq(MANAGED_OBJECT_COLLECTION), eq(ManagedObjectCollectionRepresentation.class)))
                .thenReturn(collection);
    }
}
