package com.cumulocity.lpwan.platform.repository;

import com.cumulocity.lpwan.exception.LpwanUserNotFoundException;
import com.cumulocity.lpwan.lns.connection.model.LnsConnectionDeserializer;
import com.cumulocity.lpwan.platform.service.LpwanUserPasswordService;
import com.cumulocity.microservice.context.inject.TenantScope;
import com.cumulocity.model.ID;
import com.cumulocity.model.event.CumulocityAlarmStatuses;
import com.cumulocity.model.idtype.GId;
import com.cumulocity.rest.representation.alarm.AlarmRepresentation;
import com.cumulocity.rest.representation.identity.ExternalIDRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.rest.representation.user.GroupRepresentation;
import com.cumulocity.rest.representation.user.UserMediaType;
import com.cumulocity.rest.representation.user.UserReferenceRepresentation;
import com.cumulocity.rest.representation.user.UserRepresentation;
import com.cumulocity.sdk.client.RestConnector;
import com.cumulocity.sdk.client.SDKException;
import com.cumulocity.sdk.client.alarm.AlarmApi;
import com.cumulocity.sdk.client.alarm.AlarmCollection;
import com.cumulocity.sdk.client.alarm.AlarmFilter;
import com.cumulocity.sdk.client.identity.IdentityApi;
import com.cumulocity.sdk.client.inventory.InventoryApi;
import com.cumulocity.sdk.client.user.UserApi;
import com.google.common.base.Supplier;
import com.google.common.collect.FluentIterable;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.Callable;

import static com.cumulocity.lpwan.platform.exceptions.ExceptionHandler.handled;
import static com.cumulocity.model.event.CumulocityAlarmStatuses.*;

@Component
@TenantScope
public class LpwanRepository {

    public static final String MICROSERVICE_ID_TYPE = "c8y_Microservice";
    public static final String ALARM_TYPE = "c8y_ProviderAccessAlarm";
    public static final String CRITICAL = "CRITICAL";

    private static final String GROUP_BY_NAME_URL_PATTERN = "/user/%s/groupByName/%s";
    private static final String ADD_USER_TO_GROUP_URL_PATTERN = "/user/%s/groups/%s/users";

    @Autowired
    private IdentityApi identityApi;

    @Autowired
    private AlarmApi alarmApi;

    @Autowired
    private InventoryApi inventoryApi;

    @Autowired
    private RestConnector restConnector;
    
    @Autowired
    private UserApi userApi;

    @Autowired
    private LpwanUserPasswordService lpwanUserPasswordService;

    public GId findOrCreateSource() {
        return findGId(MICROSERVICE_ID_TYPE, LnsConnectionDeserializer.getRegisteredAgentName()).or(new Supplier<GId>() {
            public GId get() {
                final GId source = createManagedObject(MICROSERVICE_ID_TYPE, LnsConnectionDeserializer.getRegisteredAgentName());
                createExternalId(source, MICROSERVICE_ID_TYPE, LnsConnectionDeserializer.getRegisteredAgentName());
                return source;
            }
        });
    }

    public ExternalIDRepresentation createExternalId(GId sourceId, String type, String identifier) {
        final ManagedObjectRepresentation source = new ManagedObjectRepresentation();
        source.setId(sourceId);

        final ExternalIDRepresentation externalId = new ExternalIDRepresentation();
        externalId.setManagedObject(source);
        externalId.setExternalId(identifier);
        externalId.setType(type);
        return identityApi.create(externalId);
    }

    public com.google.common.base.Optional<GId> findGId(String type, String identifier) {
        try {
            final ExternalIDRepresentation externalId = identityApi.getExternalId(new ID(type, identifier));
            return com.google.common.base.Optional.of(externalId.getManagedObject().getId());
        } catch (final SDKException ex) {
            if (ex.getHttpStatus() != 404) {
                throw ex;
            }
        }
        return com.google.common.base.Optional.absent();
    }

    public GId createManagedObject(String type, String name) {
        final ManagedObjectRepresentation managedObject = new ManagedObjectRepresentation();
        managedObject.setType(type);
        managedObject.setName(name);
        return inventoryApi.create(managedObject).getId();
    }

    public GId createAlarm(GId sourceId, String severity, String type, String text) {
        final ManagedObjectRepresentation source = new ManagedObjectRepresentation();
        source.setId(sourceId);

        final AlarmRepresentation alarm = new AlarmRepresentation();
        alarm.setSource(source);
        alarm.setSeverity(severity);
        alarm.setType(type);
        alarm.setText(text);
        alarm.setDateTime(DateTime.now());
        alarm.setStatus(ACTIVE.name());

        return alarmApi.create(alarm).getId();
    }

    public void clearAlarm(GId source, String type) {
        final Iterable<AlarmRepresentation> alarmMaybe = findAlarm(source, type, ACTIVE, ACKNOWLEDGED);
        for (final AlarmRepresentation alarm : alarmMaybe) {
            alarm.setStatus(CLEARED.name());
            alarmApi.update(alarm);
        }
    }

    public Iterable<AlarmRepresentation> findAlarm(GId source, String type, CumulocityAlarmStatuses... alarmStatuses) {
        try {
            final AlarmFilter filter = new AlarmFilter().bySource(source).byType(type);
            if (alarmStatuses != null && alarmStatuses.length > 0) {
                filter.byStatus(alarmStatuses);
            }
            final AlarmCollection alarms = alarmApi.getAlarmsByFilter(filter);
            return alarms.get().allPages();
        } catch (final SDKException ex) {
            if (ex.getHttpStatus() != 404) {
                throw ex;
            }
        }
        return FluentIterable.of();
    }

    /**
     * Creates or returns an account for a network server to authenticate with, through the user API.
     *
     * @deprecated The user API requires an e-mail address, so this invents one, and any address is a
     * password reset path into the account (DM-6988). Use
     * {@link com.cumulocity.lpwan.platform.service.LpwanDeviceUserService#getOrProvision(String)}, which
     * needs none. Kept for agents that have not migrated yet; do not add callers.
     */
    @Deprecated
    public UserRepresentation createOrGetUser(final String tenant, final String username, final String emailHost) throws LpwanUserNotFoundException {
        com.google.common.base.Optional<UserRepresentation> userOpt = handled(new Callable<UserRepresentation>() {
            @Override
            public UserRepresentation call() throws Exception {
                return userApi.getUser(tenant, username);
            }
        });
        Optional<String> userPassword;
        UserRepresentation user = new UserRepresentation();
        user.setUserName(username);
        if (userOpt.isPresent()) {
            userPassword = lpwanUserPasswordService.get();
            if (userPassword.isPresent()) {
                user.setPassword(userPassword.get());
            } else {
                throw new LpwanUserNotFoundException( String.format("Unable to find %s user credentials.", LnsConnectionDeserializer.getRegisteredAgentName().toLowerCase()));
            }
            user.setId(userOpt.get().getId());
            user.setRoles(userOpt.get().getRoles());
        } else {
            String generatedUserPassword = lpwanUserPasswordService.generatePasswordAndSave(username);
            if (generatedUserPassword != null) {
                user.setPassword(generatedUserPassword);
                user.setEmail(username.concat("@").concat(emailHost));
                user = userApi.create(restConnector.getPlatformParameters().getTenantId(), user);
                user.setPassword(generatedUserPassword);
                linkUserToGroup(tenant, username);
            }
        }
        return user;
    }

    private void linkUserToGroup(String tenantId, String username) {
        GroupRepresentation deviceGroup = findGroupByName(tenantId, "devices");
        addUserToDeviceGroup(deviceGroup.getId(), tenantId, username);
    }

    private GroupRepresentation findGroupByName(String tenantId, String groupName) {
        String path = String.format(GROUP_BY_NAME_URL_PATTERN, tenantId, groupName);
        return restConnector.get(path, UserMediaType.GROUP, GroupRepresentation.class);
    }

    private void addUserToDeviceGroup(long deviceGroupId, String tenantId, String username) {
        String path = String.format(ADD_USER_TO_GROUP_URL_PATTERN, tenantId, deviceGroupId);
        UserReferenceRepresentation userReferenceRepresentation = new UserReferenceRepresentation();
        UserRepresentation user = new UserRepresentation();
        user.setSelf("/users/" + username);
        userReferenceRepresentation.setUser(user);
        restConnector.post(path, UserMediaType.USER_REFERENCE, userReferenceRepresentation);
    }
}
