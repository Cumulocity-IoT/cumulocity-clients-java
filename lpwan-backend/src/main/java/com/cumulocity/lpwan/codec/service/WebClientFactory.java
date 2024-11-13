/*
 * Copyright (c) 2024 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */

package com.cumulocity.lpwan.codec.service;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class WebClientFactory {

    public static final Duration DEFAULT_TIMEOUT_IN_MILLIS = Duration.ofMillis(5000);

    private Duration timeout = DEFAULT_TIMEOUT_IN_MILLIS;
    private String baseUrl;
    private String contentType;
    private String accept;

    public static WebClientFactory builder() {
        return new WebClientFactory();
    }

    public WebClientFactory timeout(Duration duration) {
        this.timeout = duration;
        return this;
    }

    public WebClientFactory baseUrl(String url) {
        this.baseUrl = url;
        return this;
    }

    public WebClientFactory contentType(String mediaType) {
        this.contentType = mediaType;
        return this;
    }

    public WebClientFactory accept(String mediaType) {
        this.accept = mediaType;
        return this;
    }

    public WebClient build() {
        HttpClient httpClient = HttpClient.create().option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) timeout.toMillis())
                .responseTimeout(timeout)
                .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(timeout.toMillis(), TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(timeout.toMillis(), TimeUnit.MILLISECONDS)));
        return WebClient.builder().baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, contentType)
                .defaultHeader(HttpHeaders.ACCEPT, accept)
                .clientConnector(new ReactorClientHttpConnector(httpClient)).build();
    }
}
