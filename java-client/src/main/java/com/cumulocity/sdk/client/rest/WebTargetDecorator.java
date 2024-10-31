package com.cumulocity.sdk.client.rest;

import com.cumulocity.sdk.client.SDKException;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.JerseyInvocation;
import org.glassfish.jersey.client.JerseyWebTarget;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriBuilderException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public class WebTargetDecorator extends JerseyWebTarget {

    private final JerseyWebTarget delegate;

    private WebTargetDecorator(UriBuilder uriBuilder, JerseyWebTarget that) {
        super(uriBuilder, that);
        delegate = null;
    }

    private WebTargetDecorator(UriBuilder uriBuilder, ClientConfig clientConfig) {
        super(uriBuilder, clientConfig);
        delegate = null;
    }

    private WebTargetDecorator(JerseyWebTarget delegate) {
        super(null, delegate);
        this.delegate = delegate;
    }

    public static final WebTargetDecorator decorate(JerseyWebTarget target) {
        return new WebTargetDecorator(target);
    }

    @Override
    public URI getUri() {
        return delegate.getUri();
    }

    @Override
    public UriBuilder getUriBuilder() {
        return delegate.getUriBuilder();
    }

    @Override
    public JerseyWebTarget path(String s) {
        return delegate.path(s);
    }

    @Override
    public JerseyWebTarget resolveTemplate(String s, Object o) {
        return delegate.resolveTemplate(s, o);
    }

    @Override
    public JerseyWebTarget resolveTemplate(String s, Object o, boolean b) {
        return delegate.resolveTemplate(s, o, b);
    }

    @Override
    public JerseyWebTarget resolveTemplateFromEncoded(String s, Object o) {
        return delegate.resolveTemplateFromEncoded(s, o);
    }

    @Override
    public JerseyWebTarget resolveTemplates(Map<String, Object> map) {
        return delegate.resolveTemplates(map);
    }

    @Override
    public JerseyWebTarget resolveTemplates(Map<String, Object> map, boolean b) {
        return delegate.resolveTemplates(map, b);
    }

    @Override
    public JerseyWebTarget resolveTemplatesFromEncoded(Map<String, Object> map) {
        return delegate.resolveTemplatesFromEncoded(map);
    }

    @Override
    public JerseyWebTarget matrixParam(String s, Object... objects) {
        return delegate.matrixParam(s, objects);
    }

    @Override
    public JerseyWebTarget queryParam(String s, Object... objects) {
        return delegate.queryParam(s, objects);
    }

    @Override
    public JerseyInvocation.Builder request() {
        try {
            return delegate.request();
        } catch (IllegalStateException | UriBuilderException e) {
            throw new SDKException(400, "Illegal characters used in URL.");
        }
    }

    @Override
    public JerseyInvocation.Builder request(String... strings) {
        try {
            return delegate.request(strings);
        } catch (IllegalStateException | UriBuilderException e) {
            throw new SDKException(400, "Illegal characters used in URL.");
        }
    }

    @Override
    public JerseyInvocation.Builder request(MediaType... mediaTypes) {
        try {
            return delegate.request(mediaTypes);
        } catch (IllegalStateException | UriBuilderException e) {
            throw new SDKException(400, "Illegal characters used in URL.");
        }
    }

    @Override
    public ClientConfig getConfiguration() {
        return delegate.getConfiguration();
    }

    @Override
    public JerseyWebTarget property(String s, Object o) {
        return delegate.property(s, o);
    }

    @Override
    public JerseyWebTarget register(Class<?> aClass) {
        return delegate.register(aClass);
    }

    @Override
    public JerseyWebTarget register(Class<?> aClass, int i) {
        return delegate.register(aClass, i);
    }

    @Override
    public JerseyWebTarget register(Class<?> aClass, Class<?>... classes) {
        return delegate.register(aClass, classes);
    }

    @Override
    public JerseyWebTarget register(Class<?> aClass, Map<Class<?>, Integer> map) {
        return delegate.register(aClass, map);
    }

    @Override
    public JerseyWebTarget register(Object o) {
        return delegate.register(o);
    }

    @Override
    public JerseyWebTarget register(Object o, int i) {
        return delegate.register(o, i);
    }

    @Override
    public JerseyWebTarget register(Object o, Class<?>... classes) {
        return delegate.register(o, classes);
    }

    @Override
    public JerseyWebTarget register(Object o, Map<Class<?>, Integer> map) {
        return delegate.register(o, map);
    }
}
