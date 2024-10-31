package com.cumulocity.sdk.client;

import com.cumulocity.model.authentication.CumulocityBasicCredentials;
import com.cumulocity.model.authentication.CumulocityCredentials;
import com.cumulocity.model.authentication.CumulocityOAuthCredentials;
import lombok.RequiredArgsConstructor;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import java.io.IOException;

@RequiredArgsConstructor
public class CumulocityAuthenticationFilter implements ClientRequestFilter {

    private final CumulocityCredentials credentials;

    @Override
    public void filter(ClientRequestContext ctx) throws IOException {

        CumulocityCredentials.CumulocityCredentialsVisitor<Void> visitor = new CumulocityCredentials.CumulocityCredentialsVisitor<Void>() {
            @Override
            public Void visit(CumulocityBasicCredentials credentials) {
                if (!ctx.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                    ctx.getHeaders().add(HttpHeaders.AUTHORIZATION, credentials.getAuthenticationString());
                }
                return null;
            }

            @Override
            public Void visit(CumulocityOAuthCredentials credentials) {
                switch (credentials.getAuthenticationMethod()) {
                    case COOKIE:
                        ctx.getHeaders().remove(HttpHeaders.AUTHORIZATION);
                        ctx.getHeaders().putSingle("Cookie", "authorization=" + credentials.getAuthenticationString());
                        ctx.getHeaders().putSingle("X-XSRF-TOKEN", credentials.getXsrfToken());
                        return null;
                    case HEADER:
                        ctx.getHeaders().add(HttpHeaders.AUTHORIZATION, credentials.getAuthenticationString());
                }
                return null;
            }
        };
        credentials.accept(visitor);
    }
}
