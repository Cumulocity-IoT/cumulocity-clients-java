package com.cumulocity.sdk.client.common;

import com.cumulocity.rest.representation.application.ApplicationCollectionRepresentation;
import com.cumulocity.rest.representation.application.ApplicationReferenceCollectionRepresentation;
import com.cumulocity.rest.representation.application.ApplicationReferenceRepresentation;
import com.cumulocity.rest.representation.application.ApplicationRepresentation;
import com.cumulocity.sdk.client.PlatformImpl;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import java.util.Optional;

import static com.cumulocity.rest.representation.application.ApplicationMediaType.APPLICATION_REFERENCE;

public class ApplicationApi {

    private static final String TENANT_URI = "tenant/tenants";
    private static final String APPLICATIONS_URI = "application/applications/";

    private final PlatformImpl platform;

    private final Gson gson;

    @Autowired
    public ApplicationApi(PlatformImpl platform) {
        this.platform = platform;
        gson = new Gson();
    }

    private void subscribeApplicationForTenant(Client httpClient, ApplicationRepresentation application) {
        final WebTarget tenantResource = httpClient.target(platform.getHost() + TENANT_URI + "/" + platform.getTenantId() + "/applications");
        tenantResource.request(APPLICATION_REFERENCE).accept(MediaType.ALL_VALUE).post(Entity.json(toReference(application).toJSON()));
    }

    private ApplicationReferenceRepresentation toReference(ApplicationRepresentation app) {
        final ApplicationReferenceRepresentation ref = new ApplicationReferenceRepresentation();
        final ApplicationRepresentation application = new ApplicationRepresentation();
        application.setId(app.getId());
        ref.setApplication(application);
        return ref;
    }

    private void unsubscribeApplicationForTenant(Client httpClient, String applicationId) {
        WebTarget tenantResource = httpClient.target(platform.getHost() + TENANT_URI + "/" + platform.getTenantId() + "/applications/" + applicationId);
        tenantResource.request().delete();
    }

    private Optional<ApplicationRepresentation> findCepEsper(Client httpClient) {
        final WebTarget applicationResource = httpClient.target(platform.getHost() + APPLICATIONS_URI).queryParam("pageSize", "2000");
        final String allApplications = applicationResource.request().get().readEntity(String.class);
        final ApplicationCollectionRepresentation applicationReferenceRepresentations = gson.fromJson(allApplications, ApplicationCollectionRepresentation.class);
        return applicationReferenceRepresentations.getApplications().stream()
                .filter(x -> "cep".equals(x.getName()))
                .findFirst();
    }

    private Optional<ApplicationRepresentation> findApamaSubscribedToTenant(Client httpClient) {
        final WebTarget tenantResource = httpClient.target(platform.getHost() + TENANT_URI + "/" + platform.getTenantId() + "/applications").queryParam("pageSize", "1000");
        final String allTenantApplications = tenantResource.request().get().readEntity(String.class);
        final ApplicationReferenceCollectionRepresentation applicationReferenceRepresentations = gson.fromJson(allTenantApplications, ApplicationReferenceCollectionRepresentation.class);
        return applicationReferenceRepresentations.getReferences().stream()
                .map(ApplicationReferenceRepresentation::getApplication)
                .filter(x -> "cep".equals(x.getContextPath()))
                .filter(x -> x.getName().contains("apama"))
                .findFirst();

    }
}
