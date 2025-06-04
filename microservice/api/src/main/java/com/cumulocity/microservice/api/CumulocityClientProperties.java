package com.cumulocity.microservice.api;

import com.cumulocity.sdk.client.HttpClientConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(prefix = "c8y")
public class CumulocityClientProperties {

    /**
     * Client base URL (protocol, hostname & port).
     */
    private String baseURL;

    /**
     * (Optional) HTTP proxy host.
     */
    private String proxy;

    /**
     * (Optional) HTTP proxy port.
     */
    private int proxyPort;

    /**
     * In milliseconds, 0 = infinite.
     */
    private Integer httpReadTimeout;

    /**
     * (Optional) WebSocket URL for Notifications 2.0
     */
    private String baseWebSocketURL;

    @NestedConfigurationProperty
    private HttpClientConfig httpclient = HttpClientConfig.httpConfig().build();

    public String getBaseURL() {
        return baseURL;
    }

    public void setBaseURL(String baseURL) {
        this.baseURL = baseURL;
    }

    public String getProxy() {
        return proxy;
    }

    public void setProxy(String proxy) {
        this.proxy = proxy;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    @Deprecated
    @DeprecatedConfigurationProperty(replacement = "c8y.httpclient.httpReadTimeout")
    public Integer getHttpReadTimeout() {
        return httpReadTimeout;
    }

    public void setHttpReadTimeout(Integer httpReadTimeout) {
        this.httpReadTimeout = httpReadTimeout;
    }

    public HttpClientConfig getHttpclient() {
        return httpclient;
    }

    public void setHttpclient(HttpClientConfig httpclient) {
        this.httpclient = httpclient;
    }

    public String getBaseWebSocketURL() {
        return baseWebSocketURL;
    }

    public void setBaseWebSocketURL(String baseWebSocketURL) {
        this.baseWebSocketURL = baseWebSocketURL;
    }
}
