package com.cumulocity.sdk.client.event;

import com.cumulocity.sdk.client.SDKException;

import java.io.InputStream;

/**
 * API to perform operations to create, retrieve and delete event binaries. One event can store only one file.
 */
public interface EventBinaryApi {

    /**
     * Download a stored file from given event ID.
     *
     * @param id ID of event
     * @return file (binary)
     * @throws SDKException if download fails
     */
    InputStream getEventBinary(String id) throws SDKException;

    /**
     * Upload a file (binary) with content-type of "application/octet-stream" (default) to given event ID.
     *
     * @param id ID of event
     * @param bytes file (binary) to be uploaded
     * @throws SDKException if upload fails
     */
    void createEventBinary(String id, byte[] bytes) throws SDKException;

    /**
     * Replace the attached file (binary) to given event ID.
     *
     * @param id ID of event
     * @param bytes file (binary) to be uploaded
     * @throws SDKException if upload fails
     */
    void updateEventBinary(String id, byte[] bytes) throws SDKException;

    /**
     * Remove stored file from given event ID.
     *
     * @param id ID of event
     * @throws SDKException if delete fails
     */
    void deleteEventBinary(String id) throws SDKException;
}
