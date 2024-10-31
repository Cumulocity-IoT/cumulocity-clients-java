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
package com.cumulocity.sdk.client.common;

import com.cumulocity.model.authentication.CumulocityBasicCredentials;
import com.cumulocity.model.authentication.CumulocityCredentials;
import com.cumulocity.model.authentication.CumulocityOAuthCredentials;
import com.cumulocity.rest.representation.tenant.TenantMediaType;
import com.cumulocity.sdk.client.PlatformImpl;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;

import static com.cumulocity.sdk.client.common.JavaSdkITBase.nextTenantId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TenantCreator {

    private static final String TENANT_URI = "tenant/tenants";

    private final PlatformImpl platform;

    private final Random random = new Random();

    @Autowired
    public TenantCreator(PlatformImpl platform) {
        this.platform = platform;
    }

    public void createTenant() throws IOException {
        Client httpClient = new HttpClientFactory().createClient();
        try {
            createTenant(httpClient);
        } catch (Exception e) {
            if((e.getMessage().contains("=409") && e.getMessage().contains("Conflict")) ||
                e.getMessage().toLowerCase().contains("timeout")) {
                CumulocityBasicCredentials credentials = (CumulocityBasicCredentials) platform.getCumulocityCredentials();
                credentials.setTenantId(nextTenantId());
                createTenant(httpClient);
            } else {
                throw new IllegalStateException("Tenant creation failed with reason: " + e.getMessage());
            }
        } finally {
            httpClient.close();
        }
    }

    private void createTenant(Client httpClient) {
        Response response = postNewTenant(httpClient);
        if(response.getStatus() != 201) {
            throw new IllegalStateException("Tenant creation failed with reason: " + response);
        }
    }

    public void removeTenant() throws IOException {
        Client httpClient = new HttpClientFactory().createClient();
        try {
            removeTenant(httpClient);
        } finally {
            httpClient.close();
        }
    }

    private void removeTenant(Client httpClient) {
        Response tenantResponse = deleteTenant(httpClient);
        assertThat(tenantResponse.getStatus(), is(204));
    }

    private Response postNewTenant(Client httpClient) {
        String host = platform.getHost();
        final Invocation.Builder resource = httpClient.target(host + TENANT_URI).request(TenantMediaType.TENANT_TYPE).accept(TenantMediaType.TENANT_TYPE);
        CumulocityCredentials.CumulocityCredentialsVisitor<Response> visitor = new CumulocityCredentials.CumulocityCredentialsVisitor<Response>() {
            @Override
            public Response visit(CumulocityBasicCredentials credentials) {
                String tenantJson = "{ " +
                        "\"id\": \"" + platform.getTenantId() + "\", " +
                        "\"domain\": \"sample-tenant-" + random.nextInt(10000) + ".cumulocity.com\", " +
                        "\"company\": \"sample-tenant\", " +
                        "\"adminName\": \"" + credentials.getUsername() + "\", " +
                        "\"adminPass\": \"" + credentials.getPassword() + "\", " +
                        "\"adminEmail\": \"admin@sample-tenant.com\" " +
                        "}";
                Response result = resource.post(Entity.json(tenantJson));
                String newTenant = result.readEntity(String.class);
                Gson gson = new Gson();
                HashMap map = gson.fromJson(newTenant, HashMap.class);
                String newTenantId = (String) map.get("id");
                ((CumulocityBasicCredentials) platform.getCumulocityCredentials()).setTenantId(newTenantId);
                return result;
            }

            @Override
            public Response visit(CumulocityOAuthCredentials credentials) {
                throw new IllegalStateException("Cannot use credentials other than oauth yet");
            }
        };
        return platform.getCumulocityCredentials().accept(visitor);
    }

    private Response deleteTenant(Client httpClient) {
        String host = platform.getHost();
        WebTarget tenantResource = httpClient.target(host + TENANT_URI + "/" + platform.getTenantId()).queryParam("synchronous", "true");
        return tenantResource.request().delete();
    }
}
