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

package com.cumulocity.sdk.client;

import com.cumulocity.model.authentication.CumulocityCredentials;
import com.cumulocity.model.authentication.CumulocityLogin;

public class PlatformParameters {

    public final static int DEFAULT_PAGE_SIZE = 5;

    private String host;

    private String tenantId;

    private String user;

    private String password;

    private CumulocityLogin cumulocityLogin;

    private String proxyHost;

    private String applicationKey;

    private int proxyPort = -1;

    private String proxyUserId;

    private String proxyPassword;
    
    private boolean requireResponseBody = true;

    private int pageSize = DEFAULT_PAGE_SIZE;
    
    public PlatformParameters() {
        //empty constructor for spring based initialization
    }

    public PlatformParameters(String host, CumulocityCredentials credentials) {
        setMandatoryFields(host, credentials);
    }

    private void setMandatoryFields(String host, CumulocityCredentials credentials) {
        if (host.charAt(host.length() - 1) != '/') {
            host = host + "/";
        }
        this.host = host;
        this.tenantId = credentials.getTenantId();
        this.user = credentials.getUsername();
        this.password = credentials.getPassword();
        this.applicationKey = credentials.getApplicationKey();
        this.cumulocityLogin = credentials.getLogin();
    }

    public PlatformParameters(String host, CumulocityCredentials credentials, int pageSize) {
        setMandatoryFields(host, credentials);
        this.pageSize = pageSize;
    }

    public int getPageSize() {
        return pageSize;
    }

    public String getHost() {
        return host;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    public String getProxyUserId() {
        return proxyUserId;
    }

    public void setProxyUserId(String proxyUserId) {
        this.proxyUserId = proxyUserId;
    }

    public String getProxyPassword() {
        return proxyPassword;
    }

    public void setProxyPassword(String proxyPassword) {
        this.proxyPassword = proxyPassword;
    }

    public String getApplicationKey() {
        return applicationKey;
    }

    public void setApplicationKey(String applicationKey) {
        this.applicationKey = applicationKey;
    }

    public void setRequireResponseBody(boolean requireResponseBody) {
        this.requireResponseBody = requireResponseBody;
    }

    public boolean requireResponseBody() {
        return requireResponseBody;
    }

    public String getPrincipal() {
        return cumulocityLogin.toLoginString();
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
