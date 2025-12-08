package com.cumulocity.sdk.client;

import com.cumulocity.sdk.client.rest.WebTargetDecorator;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyWebTarget;
import org.glassfish.jersey.internal.util.collection.UnsafeValue;
import org.glassfish.jersey.uri.internal.JerseyUriBuilder;

import javax.net.ssl.SSLContext;
import java.net.*;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
public class CumulocityHttpClient extends JerseyClient {

    private PlatformParameters platformParameters;

    CumulocityHttpClient(ClientConfig clientConfig) {
        super(clientConfig, (UnsafeValue<SSLContext, IllegalStateException>) null,null);
    }

    public void setPlatformParameters(PlatformParameters platformParameters) {
        this.platformParameters = platformParameters;
    }

    @Override
    public JerseyWebTarget target(String path) {
        JerseyWebTarget resource;
        try {
            resource = super.target(resolvePath(path));
            resource = WebTargetDecorator.decorate(resource);
        } catch (IllegalArgumentException | URISyntaxException ex) {
            log.error("Error occurred when serializing the target URL", ex);
            throw new SDKException(400, "Illegal characters used in URL.");
        }
        return resource;
    }

    protected String resolvePath(String path) throws URISyntaxException {
        URI baseUri = new URI(platformParameters.getHost());
        URI receivedUri = new URI(path);

        JerseyUriBuilder resolvedUriBuilder = new JerseyUriBuilder().uri(receivedUri);

        if (platformParameters.isForceInitialHost() || isBlank(receivedUri.getHost())) {
            resolvedUriBuilder.scheme(baseUri.getScheme());
            resolvedUriBuilder.host(baseUri.getHost());
            resolvedUriBuilder.port(baseUri.getPort());

            if (receivedPathMissingBasePath(baseUri, receivedUri)) {
                prependBasePath(resolvedUriBuilder, baseUri, receivedUri);
            }
        }
        return resolvedUriBuilder.toString();
    }

    private static boolean receivedPathMissingBasePath(URI baseUri, URI receivedUri) {
        String basePath = baseUri.getRawPath();
        if (basePath.isEmpty()) {
            return false;
        }

        String receivedPath = receivedUri.getRawPath();
        return !receivedPath.startsWith(basePath);
    }

    private static void prependBasePath(JerseyUriBuilder resolvedUriBuilder, URI baseUri, URI receivedUri) {
        resolvedUriBuilder.replacePath(baseUri.getRawPath());
        resolvedUriBuilder.path(receivedUri.getRawPath());
    }
}
