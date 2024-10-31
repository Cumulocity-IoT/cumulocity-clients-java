package com.cumulocity.sdk.client;


import com.cumulocity.sdk.client.rest.WebTargetDecorator;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyInvocation;
import org.glassfish.jersey.client.JerseyWebTarget;
import org.glassfish.jersey.internal.util.collection.UnsafeValue;

import javax.net.ssl.SSLContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CumulocityHttpClient extends JerseyClient {

    private final Pattern hostPattern = Pattern.compile("((http|https):\\/\\/.+?)(\\/|\\?|$)");

    private PlatformParameters platformParameters;

    CumulocityHttpClient(ClientConfig clientConfig) {
        super(clientConfig, (UnsafeValue<SSLContext, IllegalStateException>) null,null);
    }

    @Override
    public JerseyWebTarget target(String path) {
        JerseyWebTarget resource;
        try {
            resource = super.target(resolvePath(path));
            resource = WebTargetDecorator.decorate(resource);
        } catch (IllegalArgumentException ex) {
            throw new SDKException(400, "Illegal characters used in URL.");
        }
        return resource;
    }

    protected String resolvePath(String path) {
        if (path.startsWith("/")) {
            path = getInitialHost() + path;
        }
        return platformParameters.isForceInitialHost() ? insertInitialHost(path) : path;
    }

    public void setPlatformParameters(PlatformParameters platformParameters) {
        this.platformParameters = platformParameters;
    }

    private String insertInitialHost(String path) {
        Matcher matcher = hostPattern.matcher(path);
        if (matcher.find()) {
            String capturedHost = matcher.group(1);
            return path.replace(capturedHost, getInitialHost());
        }
        return path;
    }

    private String getInitialHost() {
        String initialHost = platformParameters.getHost();
        if(initialHost.endsWith("/")) {
            initialHost = initialHost.substring(0, initialHost.length() - 1);
        }
        return initialHost;
    }

}
