package com.cumulocity.lpwan.platform.repository;

import com.cumulocity.lpwan.exception.LpwanUserManagementNotPermittedException;
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
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.cumulocity.model.event.CumulocityAlarmStatuses.*;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;

@Slf4j
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
    @Qualifier("userRestConnector")
    private RestConnector userRestConnector;

    @Autowired
    @Qualifier("userUserApi")
    private UserApi userUserApi;

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
     * <p>
     * User API calls run with the rights of the calling user, not the agent's service user, so call this only
     * on the thread of the request that triggered it - never from {@code @EventListener} or
     * {@code @Scheduled} code, which has no user context. Once the account exists this makes no user API
     * call at all, so any user can register devices; creating it needs user management admin rights.
     *
     * @throws LpwanUserNotFoundException                if the account exists but its password is not stored
     * @throws LpwanUserManagementNotPermittedException if the account has to be created and the calling user
     *                                                  may not
     * @deprecated Stores the password under {@code ${application.name}}, where the platform does not decrypt it
     * for the agent whenever the application is deployed under a name other than its context path, and derives the
     * e-mail address from a host the agent has to own. Use
     * {@link #createOrGetUser(String, String, String, String)}. Kept for agents that have not migrated yet; do
     * not add callers.
     */
    @Deprecated
    public UserRepresentation createOrGetUser(final String tenant, final String username, final String emailHost) throws LpwanUserNotFoundException {
        UserRepresentation user = new UserRepresentation();
        user.setUserName(username);
        Optional<String> storedPassword = lpwanUserPasswordService.get();
        if (storedPassword.isPresent()) {
            // The password is stored only once the account has been created, so it marks the account as
            // existing without asking the user API, which a caller with inventory rights only may not read.
            user.setId(username);
            user.setPassword(storedPassword.get());
            return user;
        }

        String generatedPassword = lpwanUserPasswordService.generatePassword();
        user.setPassword(generatedPassword);
        user.setEmail(username.concat("@").concat(emailHost));
        user = createUser(tenant, user);
        try {
            linkUserToGroup(tenant, username);
            lpwanUserPasswordService.savePassword(generatedPassword);
        } catch (RuntimeException e) {
            // Without its stored password the account would block every further attempt with a conflict.
            discardUser(tenant, username, e);
            throw e instanceof SDKException && ((SDKException) e).getHttpStatus() == SC_FORBIDDEN
                    ? new LpwanUserManagementNotPermittedException(e)
                    : e;
        }
        user.setPassword(generatedPassword);
        return user;
    }

    /**
     * Creates or returns the account a network server authenticates with, through the user API.
     * <p>
     * User API calls run with the rights of the calling user, not the agent's service user, so call this only
     * on the thread of the request that triggered it - never from {@code @EventListener} or {@code @Scheduled}
     * code, which has no user context. Once the account exists this makes no user API call at all; creating it
     * needs user management admin rights. Like {@link #createOrGetUser(String, String, String)}, the account is
     * added to the {@code devices} group; it gets no role of its own - the agent grants the one it needs.
     * <p>
     * The password is stored for this user name alone, so an agent can create a successor next to an account it
     * still uses, and a password an earlier version stored under {@code ${application.name}} is never taken for
     * this account's.
     *
     * @param credentialsCategory the tenant option category to store the password under, which has to be the
     *                            microservice's settings category - its application's context path - see
     *                            {@link LpwanUserPasswordService#getDeviceUser(String, String)}
     * @param email               the account's e-mail address. The user API requires one, and it is a password
     *                            reset path into the account, so pass one that can never be delivered, such as
     *                            an address in the reserved {@code .invalid} top-level domain. The platform
     *                            requires it to be unique in the tenant.
     * @return the account, with its password set
     * @throws LpwanUserNotFoundException               if the account, or another one with this e-mail address,
     *                                                  exists but no readable password is stored - see
     *                                                  {@link #deleteUser(String, String, String)}
     * @throws LpwanUserManagementNotPermittedException if the account has to be created and the calling user
     *                                                  may not
     */
    public UserRepresentation createOrGetUser(String tenant, String credentialsCategory, String username, String email) throws LpwanUserNotFoundException {
        UserRepresentation user = new UserRepresentation();
        user.setUserName(username);
        Optional<String> storedPassword = lpwanUserPasswordService.getPassword(credentialsCategory, username);
        if (storedPassword.isPresent()) {
            // The password is stored only once the account has been created, so it marks the account as
            // existing without asking the user API.
            user.setId(username);
            user.setPassword(storedPassword.get());
            return user;
        }

        String generatedPassword = lpwanUserPasswordService.generatePassword();
        user.setPassword(generatedPassword);
        user.setEmail(email);
        // No id: the platform assigns it, and refuses a create that sets one.
        createUser(tenant, user);
        try {
            linkUserToGroup(tenant, username);
            lpwanUserPasswordService.savePassword(credentialsCategory, username, generatedPassword);
        } catch (RuntimeException e) {
            // Without its stored password the account would block every further attempt with a conflict.
            discardUser(tenant, username, e);
            throw e instanceof SDKException && ((SDKException) e).getHttpStatus() == SC_FORBIDDEN
                    ? new LpwanUserManagementNotPermittedException(e)
                    : e;
        }
        user.setId(username);
        return user;
    }

    /**
     * Deletes the account and forgets its stored password, so that
     * {@link #createOrGetUser(String, String, String, String)} creates it from scratch - for an account whose
     * password is lost or no longer accepted. Anything the account was configured in has to be given the new
     * password afterwards. An account that is already gone is not an error. Call only on a request thread, like
     * {@link #createOrGetUser(String, String, String, String)}.
     *
     * @throws LpwanUserManagementNotPermittedException if the calling user may not delete the account
     */
    public void deleteUser(String tenant, String credentialsCategory, String username) {
        try {
            userUserApi.delete(tenant, username);
            log.info("Deleted the user {}", username);
        } catch (SDKException e) {
            if (e.getHttpStatus() == SC_FORBIDDEN) {
                throw new LpwanUserManagementNotPermittedException(e);
            }
            if (e.getHttpStatus() != SC_NOT_FOUND) {
                throw e;
            }
        }
        lpwanUserPasswordService.deletePassword(credentialsCategory, username);
    }

    private UserRepresentation createUser(String tenant, UserRepresentation user) throws LpwanUserNotFoundException {
        try {
            return userUserApi.create(tenant, user);
        } catch (SDKException e) {
            if (e.getHttpStatus() == SC_FORBIDDEN) {
                throw new LpwanUserManagementNotPermittedException(e);
            }
            if (e.getHttpStatus() == SC_CONFLICT) {
                throw new LpwanUserNotFoundException(String.format("Unable to find %s user credentials.",
                        LnsConnectionDeserializer.getRegisteredAgentName().toLowerCase()), e);
            }
            throw e;
        }
    }

    private void discardUser(String tenant, String username, Exception cause) {
        try {
            userUserApi.delete(tenant, username);
            log.warn("Could not finish setting up the user {}, so it was removed again. The next attempt "
                    + "creates it from scratch.", username, cause);
        } catch (Exception e) {
            log.error("Could not finish setting up the user {}, and removing it again failed too. Delete it "
                    + "by hand, otherwise every further attempt fails because its password is not stored.",
                    username, e);
        }
    }

    private void linkUserToGroup(String tenantId, String username) {
        GroupRepresentation deviceGroup = findGroupByName(tenantId, "devices");
        addUserToDeviceGroup(deviceGroup.getId(), tenantId, username);
    }

    private GroupRepresentation findGroupByName(String tenantId, String groupName) {
        String path = String.format(GROUP_BY_NAME_URL_PATTERN, tenantId, groupName);
        return userRestConnector.get(path, UserMediaType.GROUP, GroupRepresentation.class);
    }

    private void addUserToDeviceGroup(long deviceGroupId, String tenantId, String username) {
        String path = String.format(ADD_USER_TO_GROUP_URL_PATTERN, tenantId, deviceGroupId);
        UserReferenceRepresentation userReferenceRepresentation = new UserReferenceRepresentation();
        UserRepresentation user = new UserRepresentation();
        user.setSelf("/users/" + username);
        userReferenceRepresentation.setUser(user);
        userRestConnector.post(path, UserMediaType.USER_REFERENCE, userReferenceRepresentation);
    }
}
