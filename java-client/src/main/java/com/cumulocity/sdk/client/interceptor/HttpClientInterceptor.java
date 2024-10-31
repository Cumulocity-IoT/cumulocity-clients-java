package com.cumulocity.sdk.client.interceptor;


import jakarta.ws.rs.client.Invocation;

/**
 * Interceptor will ce called before http method will be executed
 */
public interface HttpClientInterceptor {

    Invocation.Builder apply(Invocation.Builder builder);

}
