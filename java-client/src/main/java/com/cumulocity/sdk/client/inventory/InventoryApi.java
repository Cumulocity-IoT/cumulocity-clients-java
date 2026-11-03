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

import java.util.List;

import com.cumulocity.model.idtype.GId;
import com.cumulocity.rest.representation.inventory.ManagedObjectCollectionRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.rest.representation.inventory.SupportedMeasurementsRepresentation;
import com.cumulocity.rest.representation.inventory.SupportedSeriesRepresentation;
import com.cumulocity.sdk.client.BulkRequestException;
import com.cumulocity.sdk.client.QueryParam;
import com.cumulocity.sdk.client.SDKException;

/**
 * API for creating, and retrieving managed objects resources from the platform. Delete and update functionality is implemented on the
 * returned managed object.
 */
public interface InventoryApi {

    /**
     * Returns the Managed Object of the Resource.
     *
     * @param id Managed Object General Identifier
     * @return ManagedObjectRepresentation
     * @throws SDKException if query failed
     */
    ManagedObjectRepresentation get(GId id) throws SDKException;

    /**
     * Returns the Managed Object of the Resource. Using additional query params e.g. withChildren=false to optimize access time.
     *
     * @param id Managed Object General Identifier
     * @param params query parameters
     * @return ManagedObjectRepresentation
     * @throws SDKException if query failed
     */
    ManagedObjectRepresentation get(GId id, QueryParam ... params) throws SDKException;

    /**
     * Deletes the Managed Object from the Cumulocity Server.
     *
     * @param id Managed Object General Identifier
     * @throws SDKException if the managed object could not be deleted
     */
    void delete(GId id) throws SDKException;

    /**
     * Deletes the Managed Object and all child devices and child assets recursively from the Cumulocity Server.
     *
     * @param id Managed Object General Identifier
     * @throws SDKException if the managed object could not be deleted
     */
    void deleteCascade(GId id) throws SDKException;

    /**
     * This update the ManagedObject for the operationCollection. Cannot update the ID.
     *
     * @param managedObjectRepresentation managed object representation
     * @return ManagedObjectRepresentation updated ManagedObject.
     * @throws SDKException if the managed object could not be updated
     */
    ManagedObjectRepresentation update(ManagedObjectRepresentation managedObjectRepresentation) throws SDKException;

    /**
     * Updates multiple managed objects in the platform with a single request. Every managed object must have its id set.
     * <p>
     * A bulk request contains at most 50 managed objects, and every id may occur only once. Updating {@code externalIds} is not
     * supported; use the Identity API instead. All managed objects are validated, and checked to exist, before anything is updated.
     * If a check fails, a {@link BulkRequestException} is thrown that identifies the failed managed object, and nothing is updated.
     * <p>
     * If the request fails while the managed objects are updated, for example because the datastore fails, part of them may have been
     * updated. Retrieve the managed objects by their ids to find out their current state.
     *
     * @param managedObjects the managed objects to be updated
     * @return the updated managed objects
     * @throws BulkRequestException if the request failed for one of the managed objects
     * @throws SDKException         if the managed objects could not be updated
     */
    ManagedObjectCollectionRepresentation updateBulk(ManagedObjectCollectionRepresentation managedObjects) throws SDKException;


    /**
     * Gets managed object resource by id. To get the managed object representation you have to call {@code get()} on the returned resource.
     *
     * @param gid id of the managed object to search for
     * @return the managed object resource associated with the given id
     * @throws SDKException if the query failed
     */
    ManagedObject getManagedObjectApi(GId gid) throws SDKException;

    /**
     * Creates managed object in the platform. The id of the managed object must not be set, since it will be generated by the platform
     *
     * @param managedObject the managed object to be created
     * @return the created managed object with the generated id
     * @throws SDKException if the managed object could not be created
     */
    ManagedObjectRepresentation create(ManagedObjectRepresentation managedObject) throws SDKException;

    /**
     * Creates multiple managed objects in the platform with a single request. The ids of the managed objects must not be set, since they
     * will be generated by the platform.
     * <p>
     * A bulk request contains at most 50 managed objects. External ids can be assigned through the {@code externalIds} field of each
     * managed object, but every external id may occur only once in the request. All managed objects are validated before anything is
     * created, so a failed validation creates nothing.
     * <p>
     * If the request fails while the managed objects are written, for example because a concurrent request binds the same external id,
     * part of the managed objects may have been created. In this case a {@link BulkRequestException} is thrown. Its
     * {@link BulkRequestException#getFragmentType() fragmentType} names a fragment that each created managed object carries with its
     * position in the request. To complete the request without duplicates:
     * <ol>
     * <li>Find the created managed objects with {@code getManagedObjectsByFilter(new InventoryFilter().byFragmentType(fragmentType))}.
     * If the lookup fails, retry the lookup, never the bulk request.</li>
     * <li>Send again only the positions that the lookup did not return. This includes
     * {@link BulkRequestException#getFailedIndex() failedIndex}, which may have been created.</li>
     * <li>A managed object that the lookup returned, including the one at {@code failedIndex}, may be missing its external ids or its
     * group assignment, for example if the failure was caused by one of its external ids. Do not create it again. Bind the missing external
     * ids through the Identity API instead.</li>
     * </ol>
     *
     * @param managedObjects the managed objects to be created
     * @return the created managed objects with their generated ids, in the order of the request
     * @throws BulkRequestException if the request failed for one of the managed objects
     * @throws SDKException         if the managed objects could not be created
     */
    ManagedObjectCollectionRepresentation createBulk(ManagedObjectCollectionRepresentation managedObjects) throws SDKException;

    /**
     * Gets the all the managed object in the platform
     *
     * @return collection of managed objects with paging functionality
     * @throws SDKException if the query failed
     */
    ManagedObjectCollection getManagedObjects() throws SDKException;

    /**
     * Gets the managed objects from the platform based on specified filter. Query based on {@code type} and {@code fragmentType} is
     * not supported.
     *
     * @param filter the filter criteria(s)
     * @return collection of managed objects matched by the filter with paging functionality
     * @throws SDKException             if the query failed
     * @throws IllegalArgumentException if both {@code type} and {@code fragmentType} are specified in the filter
     */
    ManagedObjectCollection getManagedObjectsByFilter(InventoryFilter filter) throws SDKException;

    /**
     * Returns the count of managed objects from the platform based on specified filter.
     *
     * @param filter the filter criteria(s)
     * @return count of managed objects matched by the filter
     * @throws SDKException             if the query failed
     * @throws IllegalArgumentException if the filter is null
     */
    Integer countManagedObjectsByFilter(InventoryFilter filter) throws SDKException;

    /**
     * Returns supported measurements of the Managed Object specified by its id
     *
     * @param id id of the managed object to search for
     * @return SupportedMeasurementsRepresentation
     * @throws SDKException             if managed object does not exists
     */
    SupportedMeasurementsRepresentation getSupportedMeasurements(GId id) throws SDKException;

    /**
     * Returns supported series of the Managed Object specified by its id
     *
     * @param id id of the managed object to search for
     * @return SupportedSeriesRepresentation
     * @throws SDKException             if managed object does not exists
     */
    SupportedSeriesRepresentation getSupportedSeries(GId id) throws SDKException;


    /**
     * Gets the managed objects from the platform based on the given ids
     *
     * @param ids the list of ids of the managed objects to search for
     * @return collection of managed objects matched in order of the given ids
     * @throws SDKException if the query failed
     */
    @Deprecated
    ManagedObjectCollection getManagedObjectsByListOfIds(List<GId> ids) throws SDKException;

    /**
     * Gets managed object resource by id. To get the managed object representation you have to call {@code get()} on the returned resource.
     *
     * @param gid id of the managed object to search for
     * @return the managed object resource associated with the given id
     * @throws SDKException if the query failed
     */
    @Deprecated
    ManagedObject getManagedObject(GId gid) throws SDKException;
}
