package com.cumulocity.agent.packaging.uploadMojo.platform.client.impl;

import com.cumulocity.agent.packaging.uploadMojo.platform.client.Executor;
import com.cumulocity.agent.packaging.uploadMojo.platform.client.Request;
import com.cumulocity.agent.packaging.uploadMojo.configuration.CredentialsConfiguration;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import com.google.common.base.Throwables;
import lombok.AccessLevel;
import lombok.Getter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Proxy;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import static com.cumulocity.agent.packaging.uploadMojo.platform.client.impl.UrlUtils.concat;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Getter(value = AccessLevel.PRIVATE)
public class ApacheHttpClientExecutor implements Executor {

    private final ObjectMapper mapper = JsonMapper.builderWithJackson2Defaults()
            .enable(MapperFeature.DEFAULT_VIEW_INCLUSION)
            .changeDefaultPropertyInclusion(inclusion -> inclusion.withValueInclusion(NON_NULL)
                    .withContentInclusion(NON_NULL))
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            .build();

    private final CloseableHttpClient client;
    private final String url;
    private final Log log;

    public ApacheHttpClientExecutor(final CredentialsConfiguration credentials, Log log, MavenSession mavenSession) {
        this.log = log;
        url = UrlUtils.ensureHttpSchema(credentials.getUrl());
        client = HttpClients.custom()
                .setDefaultCredentialsProvider(new BasicCredentialsProvider() {
                    public Credentials getCredentials(AuthScope authscope) {
                        return new UsernamePasswordCredentials(credentials.getUsername(), credentials.getPassword());
                    }
                })
                .setProxy( getProxyHost( mavenSession ) )
                .build();
    }

    private HttpHost getProxyHost( MavenSession mavenSession ) {
    	try {    		
	        List<Proxy>  list = mavenSession.getSettings().getProxies();
	
	        //loop about Proxy settings
	        for ( Proxy proxy : list ) {
	        	if ( proxy.isActive() ) {
	        		String pS = proxy.getProtocol() + "://" + proxy.getHost() + ":" + proxy.getPort();
	        		log.info( "Found Proxy settings and passing to Apache Client: " + pS );
	        		return new HttpHost( proxy.getHost(), proxy.getPort(), proxy.getProtocol() );
	        	}			
			}
    	}
    	catch ( NullPointerException e ) {
    		//suppress NPEs because no proxy settings can be retrieved
    	}

    	return null;
    }

    @Override
    public <T> T execute(Request<T> request) {
        try {
            final HttpUriRequest httpRequest = build(request);
            final CloseableHttpResponse response = client.execute(httpRequest);

            if (response.getStatusLine().getStatusCode() >= 400) {
                final Map result = result(Map.class, response);
                Object message = null;
                if (result != null) {
                    message = result.get("message");
                }

                throw new RuntimeException(String.valueOf(response.getStatusLine()) + " - " + message);
            }

            return result(request.getResult(), response);
        } catch (final IOException ex) {
            throw Throwables.propagate(ex);
        }
    }

    private <T> T result(Class<T> result, CloseableHttpResponse response) throws IOException {
        if (response.getEntity() != null) {
            final String value = IOUtils.toString(response.getEntity().getContent(), Charset.forName("UTF-8"));
            if (String.class.equals(result)) {
                return (T) value;
            }
            if (StringUtils.isNotBlank(value)) {
                return mapper.readValue(value, result);
            }
        }
        return null;
    }

    private HttpUriRequest build(final Request request) throws IOException {
        if (Request.Method.GET.equals(request.getMethod())) {
            final HttpGet result = new HttpGet(concat(getUrl(), request.getPath()));
            result.setHeader("Accept", "application/json");
            return result;
        }
        if (Request.Method.DELETE.equals(request.getMethod())) {
            return new HttpDelete(concat(getUrl(), request.getPath()));
        }
        if (Request.Method.POST.equals(request.getMethod())) {
            final HttpPost result = new HttpPost(concat(getUrl(), request.getPath()));
            result.setHeader("Accept", "application/json");

            final File multipartBody = request.getMultipartBody();
            final String multipartName = request.getMultipartName();
            if (multipartBody != null) {
                final HttpEntity entity = MultipartEntityBuilder.create()
                        .addPart("file", new ProgressFileBody(multipartBody, getContentType(multipartBody), multipartName))
                        .build();
                result.setEntity(entity);
            }

            if (request.getJsonBody() != null) {
                final String string = mapper.writeValueAsString(request.getJsonBody());
                result.setHeader("Content-Type", "application/json");
                final StringEntity entity = new StringEntity(string);
                result.setEntity(entity);
            }
            return result;
        }

        throw new IllegalArgumentException("Unsupported method " + request.getMethod());
    }

    private ContentType getContentType(File file) {
        if (file.getName().endsWith("zip")) {
            return ContentType.create("application/zip");
        }
        return ContentType.DEFAULT_BINARY;
    }
}
