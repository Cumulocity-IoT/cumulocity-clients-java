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

package com.cumulocity.sdk.client.measurement;

import com.cumulocity.model.idtype.GId;
import com.cumulocity.rest.representation.measurement.MeasurementCollectionRepresentation;
import com.cumulocity.rest.representation.measurement.MeasurementRepresentation;
import com.cumulocity.sdk.client.PagedCollectionResource;
import com.cumulocity.sdk.client.SDKException;

/**
 * API for creating, deleting and retrieving measurements from the platform.
 */
public interface MeasurementApi {

    /**
     * Gets measurement by id
     *
     * @param gid id of the measurement to search for
     * @return the measurement with the given id
     * @throws SDKException if the measurement is not found or if the query failed
     */
    MeasurementRepresentation getMeasurement(GId gid) throws SDKException;

    /**
     * Creates measurement in the platform. The id of the measurement must not be set, since it will be generated by the platform
     *
     * @param measurement measurement to be created
     * @return the created measurement with the generated id
     * @throws SDKException if the measurement could not be created
     */
    MeasurementRepresentation create(MeasurementRepresentation measurement) throws SDKException;

    /**
     * Deletes measurement from the platform.
     * The measurement to be deleted is identified by the id within the given measurement.
     *
     * @param measurement to be deleted
     * @throws SDKException if the measurement could not be deleted
     */
    void delete(MeasurementRepresentation measurement) throws SDKException;

    /**
     * Gets the all the measurement in the platform
     *
     * @return collection of measurements with paging functionality
     * @throws SDKException if the query failed
     */
    PagedCollectionResource<MeasurementCollectionRepresentation> getMeasurements() throws SDKException;

    /**
     * Gets the measurements from the platform based on specified filter
     *
     * @param filter the filter criteria(s)
     * @return collection of measurements matched by the filter with paging functionality
     * @throws SDKException if the query failed
     */
    PagedCollectionResource<MeasurementCollectionRepresentation> getMeasurementsByFilter(MeasurementFilter filter) throws SDKException;
    
    /**
     * Deletes measurement from the platform.
     * The measurement to be deleted is identified by the id within the given measurement.
     *
     * @param measurement to be deleted
     * @throws SDKException if the measurement could not be deleted
     */
    @Deprecated
    void deleteMeasurement(MeasurementRepresentation measurement) throws SDKException;
}
