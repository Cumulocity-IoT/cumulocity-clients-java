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
package com.cumulocity.sdk.client.notification;

import static java.util.Arrays.asList;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.cometd.bayeux.client.ClientSession;
import org.cometd.bayeux.client.ClientSession.Extension;
import org.cometd.client.BayeuxClient;
import org.cometd.client.BayeuxClient.State;
import org.cometd.client.transport.ClientTransport;

import com.cumulocity.sdk.client.PlatformParameters;
import com.cumulocity.sdk.client.RestConnector;
import com.cumulocity.sdk.client.SDKException;
import org.glassfish.jersey.client.ClientProperties;

import jakarta.ws.rs.client.Client;

class DefaultBayeuxClientProvider implements BayeuxSessionProvider {

    private static final int CONNECTED_STATE_TIMEOUT = 30;

    private final PlatformParameters parameters;

    private final String endpoint;

    private Provider<Client> httpClient;

    private final Class<?> endpointDataType;

    private final UnauthorizedConnectionWatcher unauthorizedConnectionWatcher;

    private final Collection<Extension> extensions;

    public static BayeuxSessionProvider createProvider(String endpoint, PlatformParameters paramters, Class<?> endpointDataType,
                                                       UnauthorizedConnectionWatcher unauthorizedConnectionWatcher,
                                                       Extension... extensions) {
        return createProvider(endpoint, paramters, endpointDataType, createDefaultHttpProvider(paramters), unauthorizedConnectionWatcher, extensions);
    }

    private static Provider<Client> createDefaultHttpProvider(final PlatformParameters parameters) {
        return () -> {
            final Client client = RestConnector.createURLConnectionClient(parameters);
            client.property(ClientProperties.CONNECT_TIMEOUT, 0);
            client.property(ClientProperties.READ_TIMEOUT, 0);
            return client;
        };
    }

    public static BayeuxSessionProvider createProvider(final String endpoint, final PlatformParameters parameters,
                                                       Class<?> endpointDataType, final Provider<Client> httpClient,
                                                       UnauthorizedConnectionWatcher unauthorizedConnectionWatcher, Extension... extensions) {
        return new DefaultBayeuxClientProvider(endpoint, parameters, endpointDataType, httpClient, unauthorizedConnectionWatcher, extensions);
    }

    public DefaultBayeuxClientProvider(String endpoint, PlatformParameters parameters, Class<?> endpointDataType,
                                       Provider<Client> httpClient, UnauthorizedConnectionWatcher unauthorizedConnectionWatcher,
                                       Extension... extensions) {
        this.parameters = parameters;
        this.endpoint = endpoint;
        this.httpClient = httpClient;
        this.endpointDataType = endpointDataType;
        this.unauthorizedConnectionWatcher = unauthorizedConnectionWatcher;
        this.extensions = asList(extensions);
    }

    @Override
    public ClientSession get() throws SDKException {
        return openSession(createSession());
    }

    private BayeuxClient createSession() throws SDKException {
        final BayeuxClient session = new BayeuxClient(buildUrl(), createTransport(httpClient));
        for (Extension extension : extensions) {
            session.addExtension(extension);
        }
        return session;
    }

    private BayeuxClient openSession(final BayeuxClient bayeuxClient) throws SDKException {
        bayeuxClient.handshake();
        boolean handshake = bayeuxClient.waitFor(TimeUnit.SECONDS.toMillis(CONNECTED_STATE_TIMEOUT), State.CONNECTED);
        if (!handshake) {
            throw new SDKException("unable to connect to server");
        }
        return bayeuxClient;
    }

    private String buildUrl() {
        final String host = parameters.getHost();
        return (host.endsWith("/") ? host : host + "/") + endpoint;
    }

    private ClientTransport createTransport(final Provider<Client> httpClient) {
        return new CumulocityLongPollingTransport(createTransportOptions(), httpClient, parameters, unauthorizedConnectionWatcher);
    }

    private Map<String, Object> createTransportOptions() {
        final Map<String, Object> options = new HashMap<String, Object>();
        options.put(ClientTransport.JSON_CONTEXT_OPTION, new ClientSvensonJSONContext(endpointDataType));
        return options;
    }

    @Override
    public String toString() {
        return buildUrl();
    }
}
