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
import com.cumulocity.rest.representation.inventory.ManagedObjectReferenceRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.sdk.client.SDKException;

/**
 * Java Interface to call the Cumulocity Inventory Rest API.
 */
public interface ManagedObject {

    /**
     * Returns the Managed Object of the Resource.
     *
     * @return ManagedObjectRepresentation
     * @throws SDKException if Managed Object cannot be retrieved
     */
    @Deprecated
    ManagedObjectRepresentation get() throws SDKException;

    /**
     * Deletes the Managed Object from the Cumulocity Server.
     *
     * @throws SDKException when deletion of Managed Object fail
     */
    @Deprecated
    void delete() throws SDKException;

    /**
     * This update the ManagedObject for the operationCollection. Cannot update the ID.
     *
     * @param managedObjectRepresentation ManagedObject that will be updated
     * @return ManagedObjectRepresentation updated ManagedObject.
     * @throws SDKException when update of Managed Object fail
     */
    @Deprecated
    ManagedObjectRepresentation update(ManagedObjectRepresentation managedObjectRepresentation) throws SDKException;

    /**
     * Adds a child device to the ManagedObject.
     *
     * @param refrenceReprsentation ManagedObject reference representation
     * @return ManagedObjectReferenceRepresentation with the id of th child device.
     * @throws SDKException when child references update fail
     */
    ManagedObjectReferenceRepresentation addChildDevice(ManagedObjectReferenceRepresentation refrenceReprsentation)
            throws SDKException;

    /**
     * Adds a child device to the ManagedObject.
     *
     * @param childId child ManagedObject general identifier
     * @return ManagedObjectReferenceRepresentation with the id of th child device.
     * @throws SDKException when child references update fail
     */
    ManagedObjectReferenceRepresentation addChildDevice(GId childId)
            throws SDKException;

    /**
     * Create ManagedObject and adds as child device to the parent ManagedObject.
     *
     * @param representation ManagedObject representation
     * @return ManagedObjectRepresentation with the managed object.
     * @throws SDKException when child references update fail
     */
    ManagedObjectRepresentation addChildDevice(ManagedObjectRepresentation representation)
            throws SDKException;

    /**
     * Returns all the child Devices for the Managed Object in paged collection form.
     *
     * @return ManagedObjectReferenceCollectionRepresentation which contains all the child devices.
     * @throws SDKException when fetching child devices fail
     */
    ManagedObjectReferenceCollection getChildDevices() throws SDKException;

    /**
     * Returns the child device with the given id. If it belongs to the ManagedObject.
     *
     * @param deviceId device ManagedObject general identifier
     * @return ManagedObjectReferenceRepresentation of the child device.
     * @throws SDKException when fetching child device fail
     */
    ManagedObjectReferenceRepresentation getChildDevice(GId deviceId) throws SDKException;

    /**
     * Deletes the child device  and its relation to the managed object.
     *
     * @param deviceId device ManagedObject general identifier
     * @throws SDKException when delete of child device fail
     */
    void deleteChildDevice(GId deviceId) throws SDKException;

    /**
     * Adds a child asset to the ManagedObject.
     *
     * @param refrenceReprsentation ManagedObject reference representation
     * @return ManagedObjectReferenceRepresentation with the id of th child device.
     * @throws SDKException when child references update fail
     */
    ManagedObjectReferenceRepresentation addChildAssets(ManagedObjectReferenceRepresentation refrenceReprsentation)
            throws SDKException;

    /**
     * Adds a child asset to the ManagedObject.
     *
     * @param childId child ManagedObject general identifier
     * @return ManagedObjectReferenceRepresentation with the id of th child device.
     * @throws SDKException when child references update fail
     */
    ManagedObjectReferenceRepresentation addChildAssets(GId childId)
            throws SDKException;

    /**
     * Create ManagedObject and adds as child asset to the parent ManagedObject.
     *
     * @param representation ManagedObject representation
     * @return ManagedObjectRepresentation with the managed object.
     * @throws SDKException when child references update fail
     */
    ManagedObjectRepresentation addChildAsset(ManagedObjectRepresentation representation)
            throws SDKException;

    /**
     * Returns all the child Assets for the Managed Object  in paged collection form
     *
     * @return ManagedObjectReferenceCollectionRepresentation which contains all the child devices.
     * @throws SDKException when fetching child assets fail
     */
    ManagedObjectReferenceCollection getChildAssets() throws SDKException;

    /**
     * Returns the child Asset with the given id. If it belongs to the ManagedObject.
     *
     * @param assetId asset ManagedObject general identifier
     * @return ManagedObjectReferenceRepresentation of the child device.
     * @throws SDKException when fetching child asset fail
     */
    ManagedObjectReferenceRepresentation getChildAsset(GId assetId) throws SDKException;

    /**
     * Deletes the child Asset  and its relation to the managed object.
     *
     * @param assetId asset ManagedObject general identifier
     * @throws SDKException when deleting child asset fail
     */
    void deleteChildAsset(GId assetId) throws SDKException;

    /**
     * Adds a child addition to the ManagedObject.
     *
     * @param refrenceReprsentation ManagedObject reference representation
     * @return ManagedObjectReferenceRepresentation with the id of th child addition.
     * @throws SDKException when child references update fail
     */
    ManagedObjectReferenceRepresentation addChildAdditions(ManagedObjectReferenceRepresentation refrenceReprsentation)
            throws SDKException;

    /**
     * Adds a child addition to the ManagedObject.
     *
     * @param childId child addition ManagedObject general identifier
     * @return ManagedObjectReferenceRepresentation with the id of th child addition.
     * @throws SDKException when child references update fail
     */
    ManagedObjectReferenceRepresentation addChildAdditions(GId childId)
            throws SDKException;

    /**
     * Create ManagedObject and adds as child addition to the parent ManagedObject.
     *
     * @param representation ManagedObject representation
     * @return ManagedObjectRepresentation with the managed object.
     * @throws SDKException when child references update fail
     */
    ManagedObjectRepresentation addChildAddition(ManagedObjectRepresentation representation)
            throws SDKException;

    /**
     * Returns all the child additions for the Managed Object in paged collection form
     *
     * @return ManagedObjectReferenceCollectionRepresentation which contains all the child additions.
     * @throws SDKException when fetching child additions fail
     */
    ManagedObjectReferenceCollection getChildAdditions() throws SDKException;

    /**
     * Returns the child additions with the given id. If it belongs to the ManagedObject.
     *
     * @param additionId ManagedObject addition general identifier
     * @return ManagedObjectReferenceRepresentation of the child additions.
     * @throws SDKException when fetching child addition fail
     */
    ManagedObjectReferenceRepresentation getChildAddition(GId additionId) throws SDKException;

    /**
     * Deletes the child addition and its relation to the managed object.
     *
     * @param additionId ManagedObject addition general identifier
     * @throws SDKException when child addition reference delete faile
     */
    void deleteChildAddition(GId additionId) throws SDKException;

}
