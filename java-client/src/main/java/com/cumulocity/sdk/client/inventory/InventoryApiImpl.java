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
import com.cumulocity.sdk.client.QueryParam;
import com.cumulocity.sdk.client.RestConnector;
import com.cumulocity.sdk.client.SDKException;
import com.cumulocity.sdk.client.UrlProcessor;

import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

public class InventoryApiImpl implements InventoryApi {

    private final RestConnector restConnector;

    private final int pageSize;

    private InventoryRepresentation inventoryRepresentation;

    private UrlProcessor urlProcessor;

    public InventoryApiImpl(RestConnector restConnector, UrlProcessor urlProcessor, InventoryRepresentation inventoryRepresentation, int pageSize) {
        this.restConnector = restConnector;
        this.urlProcessor = urlProcessor;
        this.inventoryRepresentation = inventoryRepresentation;
        this.pageSize = pageSize;
    }

    @Override
    public ManagedObjectRepresentation create(ManagedObjectRepresentation representation) throws SDKException {
        return restConnector.post(getMOCollectionUrl(), InventoryMediaType.MANAGED_OBJECT, representation);
    }

    @Override
    public ManagedObjectRepresentation get(GId id) throws SDKException {
        return restConnector.get(getMOCollectionUrl() + "/" + id.getValue(), InventoryMediaType.MANAGED_OBJECT, ManagedObjectRepresentation.class);
    }

    @Override
    public ManagedObjectRepresentation get(GId id, QueryParam... params) throws SDKException {
        return restConnector.get(urlProcessor.replaceOrAddQueryParam(getMOCollectionUrl() + "/" + id.getValue(), QueryParam.asMap(params)), InventoryMediaType.MANAGED_OBJECT, ManagedObjectRepresentation.class);
    }


    @Override
    public void delete(GId id) throws SDKException {
        restConnector.delete(getMOCollectionUrl() + "/" + id.getValue());
    }

    @Override
    public void deleteCascade(GId id) throws SDKException {
        restConnector.delete(getMOCollectionUrl() + "/" + id.getValue() + "?cascade=true");
    }

    @Override
    public ManagedObjectRepresentation update(ManagedObjectRepresentation managedObjectRepresentation) throws SDKException {
        return restConnector.put(getMOCollectionUrl() + "/" + managedObjectRepresentation.getId().getValue(), InventoryMediaType.MANAGED_OBJECT, managedObjectRepresentation);
    }

    @Override
    public ManagedObject getManagedObject(GId globalId) throws SDKException {
        return getManagedObjectApi(globalId);
    }

    @Override
    public ManagedObject getManagedObjectApi(GId globalId) throws SDKException {
        validatePresent(globalId);
        String url = getMOCollectionUrl() + "/" + globalId.getValue();
        return new ManagedObjectImpl(restConnector,urlProcessor, url, pageSize);
    }

    @Override
    public ManagedObjectCollection getManagedObjects() throws SDKException {
        String url = getMOCollectionUrl();
        return new ManagedObjectCollectionImpl(restConnector, url, pageSize);
    }

    @Override
    public ManagedObjectCollection getManagedObjectsByFilter(InventoryFilter filter) throws SDKException {
        if (filter == null) {
            return getManagedObjects();
        }
        Map<String, String> params = filter.getQueryParams();
        return new ManagedObjectCollectionImpl(restConnector, urlProcessor.replaceOrAddQueryParam(getMOCollectionUrl(), params), pageSize);
    }

    @Override
    public Integer countManagedObjectsByFilter(InventoryFilter filter) throws SDKException {
        if (filter == null) {
            throw new IllegalArgumentException("Inventory filter cannot be null");
        }
        Map<String, String> params = filter.getQueryParams();
        String path = getMOCollectionUrl() + "/count";
        return restConnector.get(urlProcessor.replaceOrAddQueryParam(path, params), MediaType.APPLICATION_JSON_TYPE, Integer.class);
    }

    @Override
    public SupportedMeasurementsRepresentation getSupportedMeasurements(GId sourceId) throws SDKException {
        validatePresent(sourceId);
        String path = getMOCollectionUrl() + "/" + sourceId.getValue() + "/supportedMeasurements";
        return restConnector.get(path, MediaType.APPLICATION_JSON_TYPE, SupportedMeasurementsRepresentation.class);
    }

    @Override
    public SupportedSeriesRepresentation getSupportedSeries(GId sourceId) throws SDKException {
        validatePresent(sourceId);
        String path = getMOCollectionUrl() + "/" + sourceId.getValue() + "/supportedSeries";
        return restConnector.get(path, MediaType.APPLICATION_JSON_TYPE, SupportedSeriesRepresentation.class);
    }

    @Override
    @Deprecated
    public ManagedObjectCollection getManagedObjectsByListOfIds(List<GId> ids) throws SDKException {
        return getManagedObjectsByFilter(new InventoryFilter().byIds(ids));
    }

    private void validatePresent(GId id) {
        if ((id == null) || (id.getValue() == null)) {
            throw new SDKException("Cannot determine the Global ID Value");
        }
    }

    protected String getMOCollectionUrl() throws SDKException {
        return getInventoryRepresentation().getManagedObjects().getSelf();
    }

    private InventoryRepresentation getInventoryRepresentation() throws SDKException {
        return inventoryRepresentation;
    }
}
