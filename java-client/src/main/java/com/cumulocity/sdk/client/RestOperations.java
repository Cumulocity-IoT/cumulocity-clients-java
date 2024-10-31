package com.cumulocity.sdk.client;

import com.cumulocity.rest.representation.CumulocityMediaType;
import com.cumulocity.rest.representation.ResourceRepresentation;
import com.cumulocity.rest.representation.ResourceRepresentationWithId;
import com.cumulocity.sdk.client.buffering.Future;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.InputStream;

public interface RestOperations extends AutoCloseable {

    <T extends ResourceRepresentation> T get(String path, CumulocityMediaType mediaType, Class<T> responseType) throws SDKException;

    <T > T get(String path, MediaType mediaType, Class<T> responseType) throws SDKException;

    Response.Status getStatus(String path, CumulocityMediaType mediaType) throws SDKException;

    <T extends ResourceRepresentation> T postStream(String path, CumulocityMediaType mediaType, InputStream content,
                                                    Class<T> responseClass) throws SDKException;

    <T extends ResourceRepresentation> T postText(String path, String content, Class<T> responseClass);

    <T extends ResourceRepresentation> T putText(String path, String content, Class<T> responseClass);

    <T extends ResourceRepresentation> T putStream(String path, String contentType, InputStream content,
                                                   Class<T> responseClass);

    <T extends ResourceRepresentation> T putStream(String path, MediaType mediaType, InputStream content,
                                                   Class<T> responseClass);

    void postStream(String path, InputStream inputStream, MediaType inputStreamMediaType);

    @Deprecated
    <T extends ResourceRepresentation> T postFile(String path, T representation, byte[] bytes,
                                                  Class<T> responseClass);

    <T extends ResourceRepresentation> T postFile(String path, T representation, byte[] bytes, MediaType mediaType,
                                                  Class<T> responseClass);

    @Deprecated
    <T extends ResourceRepresentation> T postFileAsStream(String path, T representation, InputStream inputStream,
                                                          Class<T> responseClass);

    <T extends ResourceRepresentation> T postFileAsStream(String path, T representation, InputStream inputStream, MediaType mediaType,
                                                          Class<T> responseClass);

    <T extends ResourceRepresentationWithId> T put(String path, MediaType mediaType, T representation) throws SDKException;

    <T extends ResourceRepresentation> Future postAsync(String path, CumulocityMediaType mediaType, T representation)
                                            throws SDKException;

    <T extends ResourceRepresentation> Future putAsync(String path, CumulocityMediaType mediaType, T representation)
                                                    throws SDKException;

    @SuppressWarnings("unchecked")
    <T extends ResourceRepresentation> T post(String path, MediaType mediaType, T representation) throws SDKException;

    <T extends ResourceRepresentationWithId> T post(String path, MediaType mediaType, T representation) throws SDKException;

    <T extends ResourceRepresentation> void postWithoutResponse(String path, MediaType mediaType, T representation) throws SDKException;

    <Result extends ResourceRepresentation, Param extends ResourceRepresentation> Result post(
            String path,
            CumulocityMediaType contentType,
            CumulocityMediaType accept,
            Param representation,
            Class<Result> clazz);

    @SuppressWarnings("unchecked")
    <T extends ResourceRepresentation> T put(String path, MediaType mediaType, T representation) throws SDKException;

    void delete(String path) throws SDKException;
}
