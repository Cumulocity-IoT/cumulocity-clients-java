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

import com.cumulocity.model.authentication.CumulocityBasicCredentials;
import com.cumulocity.model.authentication.CumulocityCredentials;
import com.cumulocity.rest.representation.platform.PlatformApiRepresentation;
import com.cumulocity.rest.representation.platform.PlatformMediaType;
import com.cumulocity.sdk.client.alarm.AlarmApi;
import com.cumulocity.sdk.client.alarm.AlarmApiImpl;
import com.cumulocity.sdk.client.audit.AuditRecordApi;
import com.cumulocity.sdk.client.audit.AuditRecordApiImpl;
import com.cumulocity.sdk.client.devicecontrol.DeviceControlApi;
import com.cumulocity.sdk.client.devicecontrol.DeviceControlApiImpl;
import com.cumulocity.sdk.client.devicecontrol.DeviceCredentialsApi;
import com.cumulocity.sdk.client.devicecontrol.DeviceCredentialsApiImpl;
import com.cumulocity.sdk.client.event.EventApi;
import com.cumulocity.sdk.client.event.EventApiImpl;
import com.cumulocity.sdk.client.event.EventBinaryApi;
import com.cumulocity.sdk.client.event.EventBinaryApiImpl;
import com.cumulocity.sdk.client.identity.IdentityApi;
import com.cumulocity.sdk.client.identity.IdentityApiImpl;
import com.cumulocity.sdk.client.inventory.BinariesApi;
import com.cumulocity.sdk.client.inventory.BinariesApiImpl;
import com.cumulocity.sdk.client.inventory.InventoryApi;
import com.cumulocity.sdk.client.inventory.InventoryApiImpl;
import com.cumulocity.sdk.client.measurement.MeasurementApi;
import com.cumulocity.sdk.client.measurement.MeasurementApiImpl;
import com.cumulocity.sdk.client.messaging.notifications.NotificationSubscriptionApi;
import com.cumulocity.sdk.client.messaging.notifications.NotificationSubscriptionApiImpl;
import com.cumulocity.sdk.client.messaging.notifications.TokenApi;
import com.cumulocity.sdk.client.messaging.notifications.TokenApiImpl;
import com.cumulocity.sdk.client.option.SystemOptionApi;
import com.cumulocity.sdk.client.option.SystemOptionApiImpl;
import com.cumulocity.sdk.client.option.TenantOptionApi;
import com.cumulocity.sdk.client.option.TenantOptionApiImpl;
import com.cumulocity.sdk.client.user.UserApi;
import com.cumulocity.sdk.client.user.UserApiImpl;

public class PlatformImpl extends PlatformParameters implements Platform {

    private static final String PLATFORM_URL = "platform";

    public static final String CLIENT_PARAMETERS = "platformParameters";

    public static final String CUMULOCITY_PAGE_SIZE = "cumulocityPageSize";

    public static final String CUMULOCITY_PASSWORD = "cumulocityPassword";

    public static final String CUMULOCITY_USER = "cumulocityUser";

    public static final String CUMULOCITY_TENANT = "cumulocityTenant";

    public static final String CUMULOCITY_PORT = "cumulocityPort";

    public static final String CUMOLOCITY_HOST = "cumolocityHost";

    public static final String CUMOLOCITY_APPLICATION_KEY = "applicationKey";

    public static final String CUMOLOCITY_PROXY_HOST = "proxyHost";

    public static final String CUMULOCITY_PROXY_PORT = "proxyPort";

    public static final String CUMULOCITY_PROXY_USER = "proxyUser";

    public static final String CUMULOCITY_PROXY_PASSWORD = "proxyPassword";

    private PlatformApiRepresentation platformApiRepresentation;

    public PlatformImpl(String host, CumulocityCredentials credentials) {
        super(host, credentials, new ClientConfiguration());
    }

    public PlatformImpl(String host, CumulocityCredentials credentials, ClientConfiguration clientConfiguration) {
        super(host, credentials, clientConfiguration);
    }

    public PlatformImpl(String host, int port, CumulocityCredentials credentials) {
        super(getHostUrl(host, port), credentials, new ClientConfiguration());
    }

    public PlatformImpl(String host, CumulocityCredentials credentials, int pageSize) {
        super(host, credentials, new ClientConfiguration(), pageSize);
    }

    public PlatformImpl(String host, CumulocityCredentials credentials, ClientConfiguration clientConfiguration, int pageSize) {
        super(host, credentials, clientConfiguration, pageSize);
    }

    public PlatformImpl(String host, int port, CumulocityCredentials credentials, int pageSize) {
        super(getHostUrl(host, port), credentials, new ClientConfiguration(), pageSize);
    }

    @Deprecated
    public PlatformImpl(String host, String tenantId, String user, String password, String applicationKey) {
        super(host,
                CumulocityBasicCredentials.builder()
                        .tenantId(tenantId)
                        .username(user)
                        .password(password)
                        .applicationKey(applicationKey)
                        .build(),
                new ClientConfiguration());
    }

    @Deprecated
    public PlatformImpl(String host, String tenantId, String user, String password, String applicationKey, int pageSize) {
        super(host,
                CumulocityBasicCredentials.builder()
                        .tenantId(tenantId)
                        .username(user)
                        .password(password)
                        .applicationKey(applicationKey)
                        .build(),
                new ClientConfiguration(), pageSize);
    }

    public PlatformImpl() {
        //empty constructor for spring based initialization
    }

    private static String getHostUrl(String host, int port) {
        return "http://" + host + ":" + port;
    }

    /**
     * This static method creates the Platform from the system parameters.
     * <p>
     * System Properties
     * cumolocityHost     : ip address or name of the Cumulocity server
     * cumulocityPort     : port number of the Cumulocity server;
     * cumulocityTenant   : Tenant ID ;
     * cumulocityUser     : User ID ;
     * cumulocityPassword : Password ;
     * cumulocityPageSize : Page size for the paging parameters.
     * <p>
     * proxyHost          : Proxy Host Name;
     * proxyPort          : Proxy Port Name
     * proxyUser          : Proxy User Name
     * proxyPassword      : Proxy Password
     *
     * @return Platform for the handle to get other methods.
     * @throws SDKException when platform creation fails
     */

    public static Platform createPlatform() throws SDKException {
        PlatformImpl platform;
        try {
            String host = System.getProperty(CUMOLOCITY_HOST);
            int port = Integer.parseInt(System.getProperty(CUMULOCITY_PORT));
            String tenantId = System.getProperty(CUMULOCITY_TENANT);
            String user = System.getProperty(CUMULOCITY_USER);
            String password = System.getProperty(CUMULOCITY_PASSWORD);
            String applicationKey = System.getProperty(CUMOLOCITY_APPLICATION_KEY);
            if (host == null || tenantId == null || user == null || password == null) {
                throw new SDKException("Cannot Create Platform as Mandatory Param are not set");
            }
            if (System.getProperty(CUMULOCITY_PAGE_SIZE) != null) {
                int pageSize = Integer.parseInt(System.getProperty(CUMULOCITY_PAGE_SIZE));
                platform = new PlatformImpl(host, port, CumulocityBasicCredentials.builder().tenantId(tenantId).username(user).password(password).applicationKey(applicationKey).build(), pageSize);
            } else {
                platform = new PlatformImpl(host, port, CumulocityBasicCredentials.builder().tenantId(tenantId).username(user).password(password).applicationKey(applicationKey).build());
            }
            String proxyHost = System.getProperty(CUMOLOCITY_PROXY_HOST);
            int proxyPort = -1;
            if (System.getProperty(CUMULOCITY_PROXY_PORT) != null) {
                proxyPort = Integer.parseInt(System.getProperty(CUMULOCITY_PROXY_PORT));
            }
            String proxyUser = System.getProperty(CUMULOCITY_PROXY_USER);
            String proxyPassword = System.getProperty(CUMULOCITY_PROXY_PASSWORD);

            if (proxyHost != null && proxyPort > 0) {
                platform.setProxyHost(proxyHost);
                platform.setProxyPort(proxyPort);
            }

            if (proxyUser != null && proxyPassword != null) {
                platform.setProxyUserId(proxyUser);
                platform.setProxyPassword(proxyPassword);
            }
        } catch (NumberFormatException e) {
            throw new SDKException("Invalid Number :" + e.getMessage());
        }

        return platform;
    }

    @Override
    public InventoryApi getInventoryApi() throws SDKException {
        RestConnector restConnector = createRestConnector();
        return new InventoryApiImpl(restConnector, new UrlProcessor(), getPlatformApi(restConnector).getInventory(), getPageSize());
    }

    @Override
    public IdentityApi getIdentityApi() throws SDKException {
        RestConnector restConnector = createRestConnector();
        return new IdentityApiImpl(restConnector, new TemplateUrlParser(), getPlatformApi(restConnector).getIdentity(), getPageSize());
    }

    @Override
    public MeasurementApi getMeasurementApi() throws SDKException {
        RestConnector restConnector = createRestConnector();
        return new MeasurementApiImpl(restConnector, new UrlProcessor(), getPlatformApi(restConnector).getMeasurement(), getPageSize());
    }

    @Override
    public DeviceControlApi getDeviceControlApi() throws SDKException {
        RestConnector restConnector = createRestConnector();
        return new DeviceControlApiImpl(this, restConnector, new UrlProcessor(), getPlatformApi(restConnector).getDeviceControl(), getPageSize());
    }

    @Override
    public EventApi getEventApi() throws SDKException {
        RestConnector restConnector = createRestConnector();
        return new EventApiImpl(restConnector, new UrlProcessor(), getPlatformApi(restConnector).getEvent(), getPageSize());
    }

    @Override
    public EventBinaryApi getEventBinaryApi() throws SDKException {
        RestConnector restConnector = createRestConnector();
        return new EventBinaryApiImpl(restConnector, getPlatformApi(restConnector).getEvent());
    }

    @Override
    public AlarmApi getAlarmApi() throws SDKException {
        RestConnector restConnector = createRestConnector();
        return new AlarmApiImpl(restConnector, new UrlProcessor(), getPlatformApi(restConnector).getAlarm(), getPageSize());
    }

    @Override
    public AuditRecordApi getAuditRecordApi() throws SDKException {
        RestConnector restConnector = createRestConnector();
        return new AuditRecordApiImpl(restConnector, new UrlProcessor(), getPlatformApi(restConnector).getAudit(), getPageSize());
    }

    @Override
    public DeviceCredentialsApi getDeviceCredentialsApi() throws SDKException {
        RestConnector restConnector = createRestConnector();
        return new DeviceCredentialsApiImpl(this, restConnector);
    }

    @Override
    public BinariesApi getBinariesApi() throws SDKException {
        RestConnector restConnector = createRestConnector();
        return new BinariesApiImpl(restConnector, getPlatformApi(restConnector).getInventory());
    }

    @Override
    public UserApi getUserApi() throws SDKException {
        RestConnector restConnector = createRestConnector();
        return new UserApiImpl(restConnector, new TemplateUrlParser(), getPlatformApi(restConnector).getUser());
    }

    @Override
    public TenantOptionApi getTenantOptionApi() throws SDKException {
        RestConnector restConnector = createRestConnector();
        return new TenantOptionApiImpl(restConnector, getPlatformApi(restConnector).getTenant(), getPageSize());
    }

    @Override
    public SystemOptionApi getSystemOptionApi() throws SDKException {
        RestConnector restConnector = createRestConnector();
        return new SystemOptionApiImpl(restConnector, getPlatformApi(restConnector).getTenant());
    }

    @Override
    public TokenApi getTokenApi() throws SDKException {
        RestConnector restConnector = createRestConnector();
        return new TokenApiImpl(this, restConnector);
    }

    @Override
    public NotificationSubscriptionApi getNotificationSubscriptionApi() throws SDKException {
        RestConnector restConnector = createRestConnector();
        UrlProcessor urlProcessor = new UrlProcessor();
        return new NotificationSubscriptionApiImpl(restConnector, urlProcessor, getPageSize());
    }

    private synchronized PlatformApiRepresentation getPlatformApi(RestConnector restConnector) throws SDKException {
        if (platformApiRepresentation == null) {
            platformApiRepresentation = restConnector.get(platformUrl(), PlatformMediaType.PLATFORM_API, PlatformApiRepresentation.class);
        }
        return platformApiRepresentation;
    }

    private String platformUrl() {
        return getHost() + PLATFORM_URL;
    }

    @Override
    protected void finalize() throws Throwable {
        close();
    }

    @Override
    public RestConnector rest() {
        return new RestConnector(this, new ResponseParser(this.getResponseMapper(), this));
    }

}
