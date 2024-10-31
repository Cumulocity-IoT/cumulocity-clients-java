package com.cumulocity.sdk.client.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.client.spi.PostInvocationInterceptor;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;
import java.io.BufferedInputStream;

@Slf4j
public class BufferedResponseStreamInterceptor implements PostInvocationInterceptor {

    @Override
    public void afterRequest(ClientRequestContext requestContext, ClientResponseContext responseContext) {
        if (!isBufferedInputStream(responseContext)) {
            responseContext.setEntityStream(new BufferedInputStream(responseContext.getEntityStream()));
        }
    }

    @Override
    public void onException(ClientRequestContext requestContext, ExceptionContext exceptionContext) {
        log.trace("Exception occurred!");
    }

    private boolean isBufferedInputStream(final ClientResponseContext responseContext) {
        return responseContext.getEntityStream().markSupported();
    }
}
