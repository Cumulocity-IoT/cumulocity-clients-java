package com.cumulocity.microservice.subscription.repository.impl;

import com.cumulocity.microservice.subscription.repository.CredentialsSwitchingPlatform;
import com.cumulocity.model.authentication.CumulocityBasicCredentials;
import com.cumulocity.model.authentication.CumulocityCredentials;
import com.cumulocity.rest.representation.CumulocityMediaType;
import com.cumulocity.rest.representation.ResourceRepresentation;
import com.cumulocity.rest.representation.ResourceRepresentationWithId;
import com.cumulocity.rest.representation.application.ApplicationCollectionRepresentation;
import com.cumulocity.rest.representation.application.ApplicationRepresentation;
import com.cumulocity.rest.representation.application.ApplicationUserCollectionRepresentation;
import com.cumulocity.rest.representation.application.ApplicationUserRepresentation;
import com.cumulocity.sdk.client.RestOperations;
import com.cumulocity.sdk.client.SDKException;
import com.cumulocity.sdk.client.buffering.Future;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.InputStream;
import java.util.*;

import static com.cumulocity.rest.representation.application.ApplicationMediaType.APPLICATION;
import static lombok.AccessLevel.NONE;
import static org.springframework.http.HttpMethod.*;

@Getter
@Slf4j
public class FakeCredentialsSwitchingPlatform implements CredentialsSwitchingPlatform {

    @Builder
    @Value
    public static class Request {
        private final String path;
        private final HttpMethod method;
        private final MediaType mediaType;
        private final Object body;
    }

    private CumulocityCredentials credentials = CumulocityBasicCredentials.builder()
            .username("servicebootstrap")
            .password("")
            .tenantId("management")
            .build();
    private final Queue<Request> requests = Queues.newArrayDeque();

    private final Map<String, ApplicationRepresentation> applications = Maps.newHashMap();
    @Getter(NONE)
    private long generateId = 0;

    private List<ApplicationUserRepresentation> listOfCurrentApplicationUsers = new ArrayList<>();

    public Request takeRequest() {
        return requests.poll();
    }

    public Collection<Request> take(Predicate<Request> by) {
        Collection<Request> filtered = FluentIterable.from(this.requests)
                .filter(by)
                .toList();

        requests.removeAll(filtered);

        return filtered;
    }


    @Override
    public RestOperations get() {
        return new RestOperations() {

            @Override
            public <T extends ResourceRepresentation> T get(String path, CumulocityMediaType mediaType, Class<T> responseType) throws SDKException {
                Request build = Request.builder()
                        .path(path)
                        .mediaType(mediaType)
                        .method(GET)
                        .build();
                pushRequest(build);
                checkAccessTo(path);
                if (responseType.equals(ApplicationRepresentation.class)) {
                    String id = Iterables.getLast(Splitter.on('/').split(path));
                    return getById(id);
                } else if (responseType.equals(ApplicationCollectionRepresentation.class)) {
                    return getByName(path);

                } else if (responseType.equals(ApplicationUserRepresentation.class)) {
                    String id = Splitter.on('/').splitToList(path).get(5);
                    if (applications.containsKey(id)) {
                        ApplicationRepresentation app = applications.get(id);
                        return (T) bootstrapUserFor(app);
                    }

                } else if (responseType.equals(ApplicationUserCollectionRepresentation.class)) {
                    ApplicationUserCollectionRepresentation applicationUserRepresentations = new ApplicationUserCollectionRepresentation();
                    applicationUserRepresentations.setUsers(listOfCurrentApplicationUsers);
                    return (T) applicationUserRepresentations;
                }
                throw new SDKException(404, path + " not found");
            }

            private <T extends ResourceRepresentation> T getById(String id) {
                if (id.equals("currentApplication") && credentials.getUsername().startsWith("service_")) {
                    ApplicationCollectionRepresentation byName = getByName(Splitter.on('_').splitToList(credentials.getUsername()).get(1));
                    return (T) Iterables.getFirst(byName.getApplications(), null);
                }
                return (T) applications.get(id);
            }

            private <T extends ResourceRepresentation> T getByName(String path) {
                final String name = Iterables.getLast(Splitter.on('/').split(path));

                ApplicationCollectionRepresentation byName = new ApplicationCollectionRepresentation();

                FluentIterable<ApplicationRepresentation> filtered = FluentIterable.from(applications.values()).filter(new Predicate<ApplicationRepresentation>() {
                    @Override
                    public boolean apply(ApplicationRepresentation app) {
                        return app.getName().equals(name);

                    }
                });
                byName.setApplications(filtered.toList());
                return (T) byName;
            }

            private boolean pushRequest(Request request) {
                log.debug("Request {}", request);
                return requests.add(request);
            }

            private void checkAccessTo(String path) {
                if (path.contains("application/currentApplication") && credentials.getUsername().equals("servicebootstrap")) {
                    throw new SDKException(403, "Unauthorized");
                }
                if (!path.contains("application/currentApplication") && !credentials.getUsername().equals("servicebootstrap")) {
                    throw new SDKException(403, "Unauthorized");
                }
            }

            @Override
            public <T> T get(String path, MediaType mediaType, Class<T> responseType) throws SDKException {
                pushRequest(Request.builder()
                        .path(path)
                        .mediaType(mediaType)
                        .method(GET)
                        .build());
                if (responseType.equals(ApplicationRepresentation.class)) {
                    String id = Iterables.getLast(Splitter.on('/').split(path));
                    return (T) applications.get(id);
                }
                return null;
            }

            @Override
            public Response.Status getStatus(String path, CumulocityMediaType mediaType) throws SDKException {
                throw new UnsupportedOperationException("unsuported");
            }

            @Override
            public <T extends ResourceRepresentation> T postStream(String path, CumulocityMediaType mediaType, InputStream content, Class<T> responseClass) throws SDKException {
                throw new UnsupportedOperationException("unsuported");
            }

            @Override
            public <T extends ResourceRepresentation> T postText(String path, String content, Class<T> responseClass) {
                throw new UnsupportedOperationException("unsuported");
            }

            @Override
            public <T extends ResourceRepresentation> T putText(String path, String content, Class<T> responseClass) {
                throw new UnsupportedOperationException("unsuported");
            }

            @Override
            public <T extends ResourceRepresentation> T putStream(String path, String contentType, InputStream content, Class<T> responseClass) {
                throw new UnsupportedOperationException("unsuported");
            }

            @Override
            public <T extends ResourceRepresentation> T putStream(String path, MediaType mediaType, InputStream content, Class<T> responseClass) {
                throw new UnsupportedOperationException("unsuported");
            }

            @Override
            public void postStream(final String path, final InputStream inputStream, final MediaType inputStreamMediaType) {
                throw new UnsupportedOperationException("unsuported");
            }

            @Override
            public <T extends ResourceRepresentation> T postFile(String path, T representation, byte[] bytes, Class<T> responseClass) {
                throw new UnsupportedOperationException("unsuported");
            }

            @Override
            public <T extends ResourceRepresentation> T postFile(String path, T representation, byte[] bytes, MediaType mediaType, Class<T> responseClass) {
                throw new UnsupportedOperationException("unsuported");
            }

            @Override
            public <T extends ResourceRepresentation> T postFileAsStream(String path, T representation, InputStream inputStream, Class<T> responseClass) {
                throw new UnsupportedOperationException("unsuported");
            }

            @Override
            public <T extends ResourceRepresentation> T postFileAsStream(String path, T representation, InputStream inputStream, MediaType mediaType, Class<T> responseClass) {
                throw new UnsupportedOperationException("unsuported");
            }

            @Override
            public <T extends ResourceRepresentationWithId> T put(String path, MediaType mediaType, T representation) throws SDKException {
                pushRequest(Request.builder()
                        .path(path)
                        .mediaType(mediaType)
                        .method(PUT)
                        .body(representation)
                        .build());
                checkAccessTo(path);
                if (mediaType.equals(APPLICATION)) {
                    String id = Iterables.getLast(Splitter.on('/').split(path));
                    ApplicationRepresentation app = getById(id);
                    merge((ApplicationRepresentation) representation, app);

                    return (T) app;
                }
                return null;
            }

            private void merge(ApplicationRepresentation source, ApplicationRepresentation dest) {
                if (source.getRoles() != null) {
                    dest.setRoles(source.getRoles());
                }
                if (source.getRequiredRoles() != null) {
                    dest.setRequiredRoles(source.getRequiredRoles());
                }

            }

            @Override
            public <T extends ResourceRepresentation> Future postAsync(String path, CumulocityMediaType mediaType, T representation) throws SDKException {
                throw new UnsupportedOperationException("unsuported");
            }

            @Override
            public <T extends ResourceRepresentation> Future putAsync(String path, CumulocityMediaType mediaType, T representation) throws SDKException {
                throw new UnsupportedOperationException("unsuported");
            }

            @Override
            public <T extends ResourceRepresentation> T post(String path, MediaType mediaType, T representation) throws SDKException {
                pushRequest(Request.builder()
                        .path(path)
                        .mediaType(mediaType)
                        .method(POST)
                        .body(representation)
                        .build());
                checkAccessTo(path);
                if (mediaType.equals(APPLICATION)) {
                    String id = String.valueOf(generateId++);

                    applications.put(id, (ApplicationRepresentation) representation);
                    ((ApplicationRepresentation) representation).setId(id);

                    return representation;
                }
                return null;
            }

            @Override
            public <T extends ResourceRepresentationWithId> T post(String path, MediaType mediaType, T representation) throws SDKException {
                pushRequest(Request.builder()
                        .path(path)
                        .mediaType(mediaType)
                        .method(POST)
                        .body(representation)
                        .build());
                checkAccessTo(path);
                if (mediaType.equals(APPLICATION)) {
                    String id = String.valueOf(generateId++);

                    applications.put(id, (ApplicationRepresentation) representation);
                    ((ApplicationRepresentation) representation).setId(id);

                    return representation;
                }
                return null;
            }

            @Override
            public <T extends ResourceRepresentation> void postWithoutResponse(String path, MediaType mediaType, T representation) throws SDKException {
                throw new UnsupportedOperationException("unsuported");
            }

            @Override
            public <Result extends ResourceRepresentation, Param extends ResourceRepresentation> Result post(String path, CumulocityMediaType contentType, CumulocityMediaType accept, Param representation, Class<Result> clazz) {
                pushRequest(Request.builder()
                        .path(path)
                        .mediaType(contentType)
                        .method(POST)
                        .body(representation)
                        .build());
                checkAccessTo(path);
                if (contentType.equals(APPLICATION)) {
                    String id = String.valueOf(generateId++);

                    applications.put(id, (ApplicationRepresentation) representation);
                    ((ApplicationRepresentation) representation).setId(id);

                    return (Result) representation;
                }
                return null;
            }

            @Override
            public <T extends ResourceRepresentation> T put(String path, MediaType mediaType, T representation) throws SDKException {
                pushRequest(Request.builder()
                        .path(path)
                        .mediaType(mediaType)
                        .method(PUT)
                        .body(representation)
                        .build());
                checkAccessTo(path);
                if (mediaType.equals(APPLICATION)) {
                    String id = Iterables.getLast(Splitter.on('/').split(path));
                    ApplicationRepresentation app = getById(id);
                    merge((ApplicationRepresentation) representation, app);

                    return (T) app;
                }
                return null;
            }

            @Override
            public void delete(String path) throws SDKException {

            }

            @Override
            public void close() throws Exception {

            }
        };
    }

    public ApplicationUserRepresentation bootstrapUserFor(ApplicationRepresentation app) {
        return ApplicationUserRepresentation.applicationUserRepresentation()
                .tenant("any")
                .name("service_" + app.getName())
                .password("anypass")
                .build();
    }

    @Override
    public CredentialsSwitchingPlatform switchTo(CumulocityCredentials credentials) {
        this.credentials = credentials;
        return this;
    }

    public void addApplication(ApplicationRepresentation app) {
        applications.put(app.getId(), app);
    }

    public void addApplicationUserRepresentation(ApplicationUserRepresentation applicationUserRepresentation) {
        this.listOfCurrentApplicationUsers.add(applicationUserRepresentation);
    }
}
