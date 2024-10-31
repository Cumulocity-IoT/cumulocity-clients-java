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
import com.cumulocity.rest.representation.ErrorMessageRepresentation;
import com.cumulocity.rest.representation.ResourceRepresentation;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class ResponseParser {

    public static final String NO_ERROR_REPRESENTATION = "Something went wrong. Failed to parse error message.";
    private static final Logger LOG = LoggerFactory.getLogger(ResponseParser.class);
    private final List<MapperWrapper> mappers;
    private final PlatformParameters platformParameters;

    public ResponseParser(final ResponseMapper mapper, PlatformParameters platformParameters) {
        this.mappers = new ArrayList<>();
        if (mapper != null) {
            this.mappers.add(MapperWrapper.objectMapper(mapper));
        }
        this.mappers.add(MapperWrapper.nullMapper());
        this.platformParameters = platformParameters;
    }

    public ResponseParser(final ResponseMapper mapper) {
        this(mapper, null);
    }
    public ResponseParser() {
        this(null, null);
    }

    public <T extends ResourceRepresentation> T parse(Response response, Class<T> type, int... expectedStatusCodes) throws SDKException {

        checkStatus(response, expectedStatusCodes);
        return read(response, type);
    }

    public <T extends Object> T parseObject(Response response, int expectedStatusCode,
            Class<T> type) throws SDKException {

        checkStatus(response, expectedStatusCode);
        return read(response, type);
    }

    public void checkStatus(Response response, int... expectedStatusCodes) throws SDKException {
        int status = response.getStatus();
        for (int expectedStatusCode : expectedStatusCodes) {
            if (status == expectedStatusCode) {
                return;
            }
        }
        throw new SDKException(status, getErrorMessage(response, status));
    }

    protected String getErrorMessage(Response response, int status) {
        String errorMessage = "Http status code: " + status;
        String responseHeader = "";

        if (response.hasEntity()) {
            String errorRepresentation = getErrorRepresentation(response);
            if (errorRepresentation == null) {
                errorRepresentation = NO_ERROR_REPRESENTATION;
            }
            Map<String, List<String>> responseHeadermap = response.getStringHeaders();
            if (responseHeadermap != null) {
                for (Map.Entry<String, List<String>> entry : responseHeadermap.entrySet()) {
                    if (entry.getKey().equals("Forwarded")
                            || entry.getKey().equals("Location")
                            || entry.getKey().equals("Via")) {
                        responseHeader += entry.getKey() +
                                ": " + entry.getValue() + "\n";
                    }
                }
            }
            errorMessage += "\n" + errorRepresentation;
        }
        if (platformParameters != null) {
            errorMessage += "\n tenant: " + platformParameters.getTenantId() + " user: " + platformParameters.getUser();
        }
        if (responseHeader.length() > 0) {
            errorMessage += "\nHttp response header: {" + responseHeader + "}";
        }
        return errorMessage;
    }

    protected String getErrorRepresentation(Response response) {
        try {
            if (isJsonResponse(response)) {
                return read(response, ErrorMessageRepresentation.class).toString();
            } else {
                LOG.error("Failed to parse error message to json. Getting error string... ");
                LOG.error(read(response, String.class));
            }
        } catch (Exception e) {
            LOG.error("Failed to parse error message", e);
        }
        return null;
    }

    protected boolean isJsonResponse(Response response){
        MediaType contentType = response.getMediaType();
        if (contentType == null) {
            return false;
        }
        return contentType.getType().contains("application")
                && contentType.getSubtype().contains("json");
    }

    public GId parseIdFromLocation(Response response) {
        String path;
        path = response.getLocation().getPath();
        String[] pathParts = path.split("\\/");
        String gid = pathParts[pathParts.length - 1];
        return new GId(gid);
    }

    public Object write(Object object) {
        for (MapperWrapper mapper : this.mappers) {
            Object write = mapper.write(object);
            if (write != null) {
                return write;
            }
        }
        return null;
    }

    private <T> T read(Response response, Class<T> clazz) {
        for (MapperWrapper mapper : this.mappers) {
            T read = mapper.read(response, clazz);
            if (read != null) {
                return read;
            }
        }
        return null;
    }
}

@Slf4j
abstract class MapperWrapper {
    abstract Object write(Object object);
    abstract <T> T read(Response response, Class<T> clazz);

    static MapperWrapper nullMapper() {
        return new MapperWrapper() {
            Object write(Object object) {
                return object;
            }

            <T> T read(Response response, Class<T> clazz) {
                return response.readEntity(clazz);
            }
        };
    }

    static MapperWrapper objectMapper(final ResponseMapper mapper) {
        return new MapperWrapper() {
            Object write(Object object) {
                try {
                    final CharSequence result = mapper.write(object);
                    if (result != null) {
                        return result;
                    }
                } catch (final Exception ex) {
                    log.error(ex.getMessage());
                }
                return null;
            }

            <T> T read(Response response, Class<T> clazz) {
                try {
                    final T result = mapper.read((InputStream) response.getEntity(), clazz);
                    if (result != null) {
                        return result;
                    }
                } catch (final Exception ex) {
                    log.error(ex.getMessage());
                }
                return null;
            }
        };
    }
}
