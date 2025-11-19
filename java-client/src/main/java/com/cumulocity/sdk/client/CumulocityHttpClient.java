package com.cumulocity.sdk.client;

import com.cumulocity.sdk.client.rest.WebTargetDecorator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyWebTarget;
import org.glassfish.jersey.internal.util.collection.UnsafeValue;

import javax.net.ssl.SSLContext;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.http.client.utils.URLEncodedUtils.formatSegments;

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
        URIBuilder baseUri = new URIBuilder(platformParameters.getHost());
        URIBuilder resolvedUri = new URIBuilder(path);
        if (platformParameters.isForceInitialHost() || isBlank(resolvedUri.getHost())) {
            resolvedUri.setScheme(baseUri.getScheme());
            resolvedUri.setHost(baseUri.getHost());
            resolvedUri.setPort(baseUri.getPort());

            if (resolvedPathMissingBasePath(baseUri, resolvedUri)) {
                prependBasePath(baseUri, resolvedUri);
            }
        }
        return resolvedUri.toString();
    }

    private static boolean resolvedPathMissingBasePath(URIBuilder baseUri, URIBuilder resolvedUri) {
        if (baseUri.getPathSegments().isEmpty()) {
            return false;
        }
        String basePath = formatSegments(baseUri.getPathSegments(), UTF_8);
        String resolvedPath = formatSegments(resolvedUri.getPathSegments(), UTF_8);
        return !resolvedPath.startsWith(basePath);
    }

    private static void prependBasePath(URIBuilder baseUri, URIBuilder resolvedUri) {
        resolvedUri.setPathSegments(Stream.of(baseUri, resolvedUri)
                .map(URIBuilder::getPathSegments)
                .flatMap(Collection::stream)
                .filter(StringUtils::isNotBlank)
                .toList());
    }
}
