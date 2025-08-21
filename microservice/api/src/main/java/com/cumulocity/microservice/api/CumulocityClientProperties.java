package com.cumulocity.microservice.api;

import com.cumulocity.sdk.client.HttpClientConfig;
import com.cumulocity.sdk.client.notification2.config.Notifications2Properties;
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

    @NestedConfigurationProperty
    private HttpClientConfig httpclient = HttpClientConfig.httpConfig().build();

    @NestedConfigurationProperty
    private Notifications2Properties notifications2 = new Notifications2Properties();

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

    public Notifications2Properties getNotifications2() {
        return notifications2;
    }

    public void setNotifications2(Notifications2Properties notifications2) {
        this.notifications2 = notifications2;
    }
}
