package com.cumulocity.microservice.api;

import com.cumulocity.microservice.context.ContextService;
import com.cumulocity.microservice.context.credentials.Credentials;
import com.cumulocity.microservice.context.credentials.MicroserviceCredentials;
import com.cumulocity.microservice.context.credentials.UserCredentials;
import com.cumulocity.microservice.context.inject.TenantScope;
import com.cumulocity.microservice.context.inject.UserScope;
import com.cumulocity.model.authentication.CumulocityCredentials;
import com.cumulocity.model.authentication.CumulocityCredentialsFactory;
import com.cumulocity.sdk.client.*;
import com.cumulocity.sdk.client.alarm.AlarmApi;
import com.cumulocity.sdk.client.audit.AuditRecordApi;
import com.cumulocity.sdk.client.base.Supplier;
import com.cumulocity.sdk.client.devicecontrol.DeviceControlApi;
import com.cumulocity.sdk.client.devicecontrol.DeviceCredentialsApi;
import com.cumulocity.sdk.client.event.EventApi;
import com.cumulocity.sdk.client.identity.IdentityApi;
import com.cumulocity.sdk.client.inventory.BinariesApi;
import com.cumulocity.sdk.client.inventory.InventoryApi;
import com.cumulocity.sdk.client.measurement.MeasurementApi;
import com.cumulocity.sdk.client.messaging.notifications.NotificationSubscriptionApi;
import com.cumulocity.sdk.client.messaging.notifications.TokenApi;
import com.cumulocity.sdk.client.notification.NotificationSubscriberProducer;
import com.cumulocity.sdk.client.option.SystemOptionApi;
import com.cumulocity.sdk.client.option.TenantOptionApi;
import com.cumulocity.sdk.client.user.UserApi;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.ResolvableType;

import jakarta.annotation.PreDestroy;
import java.util.Optional;

@Configuration
@EnableConfigurationProperties(CumulocityClientProperties.class)
public class CumulocityClientFeature {

    // only for backwards compatibility with platform.url
    // use CumulocityClientProperties directly instead
    @Value("${platform.url:http://localhost:8181}")
    private String baseUrlFallback;

    @Autowired
    private CumulocityClientProperties clientProperties;

    @Autowired(required = false)
    private ResponseMapper responseMapper;

    @Primary
    @TenantScope
    @Configuration("tenantPlatform")
    class TenantPlatformConfig implements Platform {

        private PlatformImpl delegate;

        /**
         * Using setter injection over preferred constructor injection as due to double proxy on this class
         * (scoped & configuration) it's loosing generic parameter info on CGLib extended class constructor
         * and causing NoUniqueBeanDefinitionException looking up ContextService&lt;?&gt;.
         */
        @Autowired
        void setContextService(ContextService<MicroserviceCredentials> microserviceContextService) {
            this.delegate = platformFor(microserviceContextService::getContext);
        }

        PlatformImpl getDelegate() {
            return delegate;
        }

        @Primary
        @TenantScope
        @Bean(name = "tenantCredentials")
        public CumulocityCredentials credentials() {
            return delegate.getCumulocityCredentials();
        }

        @Primary
        @TenantScope
        @Bean(destroyMethod = "close")
        public RestConnector tenantRestConnector() {
            return delegate.createRestConnector();
        }

        @Override
        public RestOperations rest() {
            return delegate.rest();
        }

        @Override
        @Primary
        @TenantScope
        @Bean(name = {"inventoryApi", "tenantInventoryApi"})
        public InventoryApi getInventoryApi() throws SDKException {
            return delegate.getInventoryApi();
        }

        @Override
        @Primary
        @TenantScope
        @Bean(name = {"identityApi", "tenantIdentityApi"})
        public IdentityApi getIdentityApi() throws SDKException {
            return delegate.getIdentityApi();
        }

        @Override
        @Primary
        @TenantScope
        @Bean(name = {"measurementApi", "tenantMeasurementApi"})
        public MeasurementApi getMeasurementApi() throws SDKException {
            return delegate.getMeasurementApi();
        }

        @Override
        @Primary
        @TenantScope
        @Bean(name = {"deviceControlApi", "tenantDeviceControlApi"})
        public DeviceControlApi getDeviceControlApi() throws SDKException {
            return delegate.getDeviceControlApi();
        }

        @Override
        @Primary
        @TenantScope
        @Bean(name = {"alarmApi", "tenantAlarmApi"})
        public AlarmApi getAlarmApi() throws SDKException {
            return delegate.getAlarmApi();
        }

        @Override
        @Primary
        @TenantScope
        @Bean(name = {"eventApi", "tenantEventApi"})
        public EventApi getEventApi() throws SDKException {
            return delegate.getEventApi();
        }

        @Override
        @Primary
        @TenantScope
        @Bean(name = {"auditRecordApi", "tenantAuditRecordApi"})
        public AuditRecordApi getAuditRecordApi() throws SDKException {
            return delegate.getAuditRecordApi();
        }

        @Override
        @Primary
        @TenantScope
        @Bean(name = {"deviceCredentialsApi", "tenantDeviceCredentialsApi"})
        public DeviceCredentialsApi getDeviceCredentialsApi() throws SDKException {
            return delegate.getDeviceCredentialsApi();
        }

        @Override
        @Primary
        @TenantScope
        @Bean(name = {"binariesApi", "tenantBinariesApi"})
        public BinariesApi getBinariesApi() throws SDKException {
            return delegate.getBinariesApi();
        }

        @Override
        @Primary
        @TenantScope
        @Bean(name = {"userApi", "tenantUserApi"})
        public UserApi getUserApi() throws SDKException {
            return delegate.getUserApi();
        }

        @Override
        @Primary
        @TenantScope
        @Bean(name = {"tenantOptionApi", "tenantTenantOptionApi"})
        public TenantOptionApi getTenantOptionApi() throws SDKException {
            return delegate.getTenantOptionApi();
        }

        @Override
        @Primary
        @TenantScope
        @Bean(name = {"systemOptionApi", "tenantSystemOptionApi"})
        public SystemOptionApi getSystemOptionApi() throws SDKException {
            return delegate.getSystemOptionApi();
        }

        @Override
        @Primary
        @TenantScope
        @Bean(name = {"tokenApi", "tenantTokenApi"})
        public TokenApi getTokenApi() throws SDKException {
            return delegate.getTokenApi();
        }

        @Override
        @Primary
        @TenantScope
        @Bean(name = {"notificationSubscriptionApi", "tenantNotificationSubscriptionApi"})
        public NotificationSubscriptionApi getNotificationSubscriptionApi() throws SDKException {
            return delegate.getNotificationSubscriptionApi();
        }

        @Primary
        @TenantScope
        @Bean(name = {"notificationSubscriberProducer", "tenantNotificationSubscriberProducer"})
        public NotificationSubscriberProducer getNotificationSubscriberProducer() {
            return new NotificationSubscriberProducer(delegate);
        }

        @Override
        @PreDestroy
        public void close() {
            delegate.close();
        }
    }

    @UserScope
    @Configuration("userPlatform")
    class UserPlatformConfig implements Platform {

        private PlatformImpl delegate;

        /**
         * Using setter injection over preferred constructor injection as due to double proxy on this class
         * (scoped & configuration) it's loosing generic parameter info on CGLib extended class constructor
         * and causing NoUniqueBeanDefinitionException looking up ContextService&lt;?&gt;.
         */
        @Autowired
        void setContextService(ContextService<UserCredentials> userContextService) {
            this.delegate = platformFor(userContextService::getContext);
        }

        PlatformImpl getDelegate() {
            return delegate;
        }

        @UserScope
        @Bean(name = "userCredentials")
        public CumulocityCredentials credentials() {
            return delegate.getCumulocityCredentials();
        }

        @UserScope
        @Bean(destroyMethod = "close")
        public RestConnector userRestConnector() {
            return delegate.createRestConnector();
        }

        @Override
        public RestOperations rest() {
            return delegate.rest();
        }

        @Override
        @UserScope
        @Bean(name = "userInventoryApi")
        public InventoryApi getInventoryApi() throws SDKException {
            return delegate.getInventoryApi();
        }

        @Override
        @UserScope
        @Bean(name = "userIdentityApi")
        public IdentityApi getIdentityApi() throws SDKException {
            return delegate.getIdentityApi();
        }

        @Override
        @UserScope
        @Bean(name = "userMeasurementApi")
        public MeasurementApi getMeasurementApi() throws SDKException {
            return delegate.getMeasurementApi();
        }

        @Override
        @UserScope
        @Bean(name = "userDeviceControlApi")
        public DeviceControlApi getDeviceControlApi() throws SDKException {
            return delegate.getDeviceControlApi();
        }

        @Override
        @UserScope
        @Bean(name = "userAlarmApi")
        public AlarmApi getAlarmApi() throws SDKException {
            return delegate.getAlarmApi();
        }

        @Override
        @UserScope
        @Bean(name = "userEventApi")
        public EventApi getEventApi() throws SDKException {
            return delegate.getEventApi();
        }

        @Override
        @UserScope
        @Bean(name = "userAuditRecordApi")
        public AuditRecordApi getAuditRecordApi() throws SDKException {
            return delegate.getAuditRecordApi();
        }

        @Override
        @UserScope
        @Bean(name = "userDeviceCredentialsApi")
        public DeviceCredentialsApi getDeviceCredentialsApi() throws SDKException {
            return delegate.getDeviceCredentialsApi();
        }

        @Override
        @UserScope
        @Bean(name = "userBinariesApi")
        public BinariesApi getBinariesApi() throws SDKException {
            return delegate.getBinariesApi();
        }

        @Override
        @UserScope
        @Bean(name = "userUserApi")
        public UserApi getUserApi() throws SDKException {
            return delegate.getUserApi();
        }

        @Override
        @UserScope
        @Bean(name = "userTenantOptionApi")
        public TenantOptionApi getTenantOptionApi() throws SDKException {
            return delegate.getTenantOptionApi();
        }

        @Override
        @UserScope
        @Bean(name = "userSystemOptionApi")
        public SystemOptionApi getSystemOptionApi() throws SDKException {
            return delegate.getSystemOptionApi();
        }

        @Override
        @UserScope
        @Bean(name = "userTokenApi")
        public TokenApi getTokenApi() throws SDKException {
            return delegate.getTokenApi();
        }

        @Override
        @UserScope
        @Bean(name = "userNotificationSubscriptionApi")
        public NotificationSubscriptionApi getNotificationSubscriptionApi() throws SDKException {
            return delegate.getNotificationSubscriptionApi();
        }

        @UserScope
        @Bean(name = "userNotificationSubscriberProducer")
        public NotificationSubscriberProducer getNotificationSubscriberProducer() {
            return new NotificationSubscriberProducer(delegate);
        }

        @Override
        @PreDestroy
        public void close() {
            delegate.close();
        }
    }

    private PlatformImpl platformFor(Supplier<Credentials> credentialsSupplier) {
        final Credentials login = credentialsSupplier.get();
        CumulocityCredentials credentials = new CumulocityCredentialsFactory()
                .withTenant(login.getTenant())
                .withUsername(login.getUsername())
                .withPassword(login.getPassword())
                .withOAuthAccessToken(login.getOAuthAccessToken())
                .withXsrfToken(login.getXsrfToken())
                .withApplicationKey(login.getAppKey())
                .getCredentials();
        PlatformImpl platform = (PlatformImpl) PlatformBuilder.platform()
                .withBaseUrl(getBaseUrl())
                .withProxyHost(clientProperties.getProxy())
                .withProxyPort(clientProperties.getProxyPort())
                .withCredentials(credentials)
                .withTfaToken(login.getTfaToken())
                .withResponseMapper(responseMapper)
                .withForceInitialHost(true)
                .build();
        setHttpClientConfig(platform);
        return platform;
    }

    private String getBaseUrl() {
        return Optional.ofNullable(clientProperties.getBaseURL()).orElse(baseUrlFallback);
    }

    private void setHttpClientConfig(PlatformImpl platform) {
        HttpClientConfig httpClientConfig = clientProperties.getHttpclient();
        if (clientProperties.getHttpReadTimeout() != null) {
            httpClientConfig.setHttpReadTimeout(clientProperties.getHttpReadTimeout());
        }
        platform.setHttpClientConfig(httpClientConfig);
    }
}
