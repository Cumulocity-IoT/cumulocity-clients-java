package com.cumulocity.sdk.services.client;

import com.cumulocity.email.client.EmailApi;
import com.cumulocity.email.client.EmailApiImpl;
import com.cumulocity.model.authentication.CumulocityCredentials;
import com.cumulocity.sms.client.SmsMessagingApi;
import com.cumulocity.sms.client.SmsMessagingApiImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;

import static org.apache.http.client.fluent.Executor.newInstance;

@Slf4j
public class ServicesPlatformImpl implements ServicesPlatform {

    public interface CredentialsProvider {
        String getTenant();
        String getUsername();
        String getPassword();
    }

    private final String host;
    private final SmsMessagingApiImpl.CredentialsProvider credentials;

    public ServicesPlatformImpl(String host, final CumulocityCredentials credentials) {
        this(host, new CredentialsProvider() {
            public String getTenant() {
                return credentials.getTenantId();
            }

            public String getUsername() {
                return credentials.getUsername();
            }

            public String getPassword() {
                return credentials.getPassword();
            }
        });
    }

    public ServicesPlatformImpl(String host, final CredentialsProvider auth) {
        if (host.charAt(host.length() - 1) != '/') {
            host = host + "/";
        }
        this.host = host;
        this.credentials = new SmsMessagingApiImpl.CredentialsProvider() {
            @Override
            public String getTenant() {
                return auth.getTenant();
            }

            @Override
            public String getUsername() {
                return auth.getUsername();
            }

            @Override
            public String getPassword() {
                return auth.getPassword();
            }
        };
    }

    @Override
    public SmsMessagingApi getSmsMessagingApi() {
        return new SmsMessagingApiImpl(host, credentials);
    }

    @Override
    public EmailApi getEmailApi() {
        return new EmailApiImpl(host, newInstance().use(new BasicCredentialsProvider() {
            public Credentials getCredentials(AuthScope authscope) {
                return new UsernamePasswordCredentials(credentials.getTenant() + "/" + credentials.getUsername(), credentials.getPassword());
            }
        }));
    }
}
