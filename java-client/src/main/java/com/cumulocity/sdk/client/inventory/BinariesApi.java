package com.cumulocity.sdk.client.inventory;

import java.io.InputStream;

import com.cumulocity.model.idtype.GId;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.sdk.client.SDKException;

import jakarta.ws.rs.core.MediaType;

/**
 * API to perform operations to store, retrieve and delete binaries. One binary can store only one file.
 * Together with the binary, a managed object is created which acts as a metadata information for the binary.
 */
public interface BinariesApi {

    /**
     * Upload a file (binary) with content-type of "application/octet-stream" (default).
     *
     * @param container ManagedObjectRepresentation with binary metadata
     * @param bytes file (binary) to be uploaded
     * @return ManagedObjectRepresentation with binary metadata
     * @throws SDKException if upload fails
     */
    ManagedObjectRepresentation uploadFile(ManagedObjectRepresentation container, byte[] bytes) throws SDKException;

    /**
     * Upload a file (binary) with custom content-type.
     *
     * @param container ManagedObjectRepresentation with binary metadata
     * @param bytes file (binary) to be uploaded
     * @param mediaType content-type of file (binary)
     * @return ManagedObjectRepresentation with binary metadata
     * @throws SDKException if upload fails
     */
    ManagedObjectRepresentation uploadFile(ManagedObjectRepresentation container, byte[] bytes, MediaType mediaType) throws SDKException;

    /**
     * Upload a file (binary) with content-type of "application/octet-stream" (default).
     *
     * @param container ManagedObjectRepresentation with binary metadata
     * @param inputStream file (binary) to be uploaded
     * @return ManagedObjectRepresentation with binary metadata
     * @throws SDKException if upload fails
     */
    ManagedObjectRepresentation uploadFile(ManagedObjectRepresentation container, InputStream inputStream) throws SDKException;

    /**
     * Upload a file (binary) with custom content-type.
     *
     * @param container ManagedObjectRepresentation with binary metadata
     * @param inputStream file (binary) to be uploaded
     * @param mediaType content-type of file (binary)
     * @return ManagedObjectRepresentation with binary metadata
     * @throws SDKException if upload fails
     */
    ManagedObjectRepresentation uploadFile(ManagedObjectRepresentation container, InputStream inputStream, MediaType mediaType) throws SDKException;

    /**
     * Upload and replace the attached file (binary) of a specific managed object by a given ID.
     *
     * @param containerId ID of managed object
     * @param contentType content-type of file (binary)
     * @param fileStream file (binary) to be uploaded
     * @return ManagedObjectRepresentation with binary metadata
     * @throws SDKException if upload fails
     */
    ManagedObjectRepresentation replaceFile(GId containerId, String contentType, InputStream fileStream) throws SDKException;

    /**
     * Download a stored file (managed object) by a given ID.
     *
     * @param id ID of managed object
     * @return file (binary)
     * @throws SDKException if download fails
     */
    InputStream downloadFile(GId id) throws SDKException;

    /**
     * Remove a managed object and its stored file by a given ID.
     *
     * @param containerId ID of managed object
     * @throws SDKException if delete fails
     */
    void deleteFile(GId containerId) throws SDKException;

}
