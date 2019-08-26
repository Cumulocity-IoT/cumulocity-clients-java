package com.cumulocity.sdk.client;

import com.cumulocity.model.authentication.CumulocityBasicCredentials;
import com.cumulocity.model.authentication.CumulocityCredentials;
import com.cumulocity.model.authentication.CumulocityOAuthCredentials;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.filter.ClientFilter;
import lombok.RequiredArgsConstructor;

import javax.ws.rs.core.HttpHeaders;

@RequiredArgsConstructor
public class CumulocityAuthenticationFilter extends ClientFilter {

    private final CumulocityCredentials credentials;

    @Override
    public ClientResponse handle(final ClientRequest cr) throws ClientHandlerException {

        CumulocityCredentials.CumulocityCredentialsVisitor<Void> visitor = new CumulocityCredentials.CumulocityCredentialsVisitor<Void>() {
            @Override
            public Void visit(CumulocityBasicCredentials credentials) {
                if (!cr.getMetadata().containsKey(HttpHeaders.AUTHORIZATION)) {
                    cr.getMetadata().add(HttpHeaders.AUTHORIZATION, credentials.getAuthenticationString());
                }
                return null;
            }

            @Override
            public Void visit(CumulocityOAuthCredentials credentials) {
                switch (credentials.getAuthenticationMethod()) {
                    case COOKIE:
                        cr.getMetadata().remove(HttpHeaders.AUTHORIZATION);
                        cr.getHeaders().putSingle("Cookie", "authorization=" + credentials.getAuthenticationString());
                        cr.getHeaders().putSingle("X-XSRF-TOKEN", credentials.getXsrfToken());
                        return null;
                    case HEADER:
                        cr.getMetadata().add(HttpHeaders.AUTHORIZATION, credentials.getAuthenticationString());
                }
                return null;
            }
        };
        credentials.accept(visitor);

        return getNext().handle(cr);
    }

}
