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
package com.cumulocity.me.sdk.client.http;

import javax.microedition.io.HttpConnection;

import com.cumulocity.me.http.WebClient;
import com.cumulocity.me.http.WebRequestBuilder;
import com.cumulocity.me.rest.representation.CumulocityMediaType;
import com.cumulocity.me.rest.representation.ResourceRepresentation;
import com.cumulocity.me.sdk.client.PlatformParameters;
import com.cumulocity.me.util.Base64;
import com.cumulocity.me.util.StringUtils;

public class RestConnectorImpl implements RestConnector {

    private final PlatformParameters platformParameters;

    private final WebClient client;

    public RestConnectorImpl(PlatformParameters platformParameters, WebClient client) {
        this.platformParameters = platformParameters;
        this.client = client;
    }

    public ResourceRepresentation get(String path, CumulocityMediaType mediaType, Class responseEntityType) {
        return authorizedRequest(path).accept(mediaType).get(HttpConnection.HTTP_OK, responseEntityType);
    }

    public ResourceRepresentation post(String path, CumulocityMediaType mediaType, ResourceRepresentation representation) {
        WebRequestBuilder builder = authorizedRequest(path).type(mediaType);
        if (platformParameters.isRequireResponseBody()) {
            builder.accept(mediaType);
        }
        return builder.post(representation, HttpConnection.HTTP_CREATED, representation.getClass());
    }

    public ResourceRepresentation put(String path, CumulocityMediaType mediaType, ResourceRepresentation representation) {
        WebRequestBuilder builder = authorizedRequest(path).type(mediaType);
        if (platformParameters.isRequireResponseBody()) {
            builder.accept(mediaType);
        }
        return builder.put(representation, HttpConnection.HTTP_OK, representation.getClass());
    }

    public void delete(String path) {
        authorizedRequest(path).delete(HttpConnection.HTTP_NO_CONTENT, null);
    }
    
    private WebRequestBuilder authorizedRequest(String path) {
        return client.request(insertTailIfNeeded(path))
                .header(AUTHORIZATION, getBasicAuthenticationCode(platformParameters))
                .header(X_CUMULOCITY_APPLICATION_KEY, platformParameters.getApplicationKey());
    }
    
    private String getBasicAuthenticationCode(PlatformParameters platformParameters) {
        String username = platformParameters.getTenantId() + "/" + platformParameters.getUser();
        String password = platformParameters.getPassword();
        return "Basic " + new String(Base64.encode(username + ":" + password));
    }
    
    private String insertTailIfNeeded(String path) {
        int indexOf = path.indexOf("?");
        if (indexOf >= 0) {
            return StringUtils.insert(path, indexOf, "/");
        } else {
            return StringUtils.ensureTail(path, "/");
        }
    }
    
}
