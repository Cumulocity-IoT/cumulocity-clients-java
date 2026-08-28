package com.cumulocity.lpwan.platform.service;

import com.cumulocity.lpwan.exception.LpwanDeviceUserCredentialsLostException;
import com.cumulocity.lpwan.platform.service.LpwanUserPasswordService.StoredDeviceUser;
import com.cumulocity.microservice.subscription.service.MicroserviceSubscriptionsService;
import com.cumulocity.rest.representation.devicebootstrap.DeviceCredentialsRepresentation;
import com.cumulocity.rest.representation.devicebootstrap.NewDeviceRequestRepresentation;
import com.cumulocity.rest.representation.operation.DeviceControlMediaType;
import com.cumulocity.sdk.client.RestConnector;
import com.cumulocity.sdk.client.SDKException;
import com.cumulocity.sdk.client.devicecontrol.DeviceCredentialsApi;
import com.cumulocity.sdk.client.user.UserApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;

/**
 * Provisions the account an external network server uses to deliver data into Cumulocity, through the
 * platform's device registration flow. That flow needs no e-mail address, so the account has no password
 * reset path, and it creates no managed object.
 * <p>
 * Every method operates on the tenant of the current context.
 */
@Service
@Slf4j
public class LpwanDeviceUserService {

    private static final String DEVICE_USER_NAME_PREFIX = "device_";

    private static final String NEW_DEVICE_REQUEST_PATH = "/devicecontrol/newDeviceRequests/%s";

    private static final String ACCEPTED_STATUS = "ACCEPTED";

    // Device id rules, copied from core - it is a different repository and not on this classpath. Copy
    // from the validator implementations, not from the annotation messages: the message for
    // ValidUsernameCharacters omits the backslash its pattern excludes.

    /** Core: {@code UsernameCharactersValidator.validUsername}. */
    private static final Pattern VALID_USERNAME_CHARACTERS = Pattern.compile("[^\\s\\\\/+$:]+");

    /** Core: {@code NotControlCharacterValidator.NO_CONTROL_CHARACTER}. {@code \p{Cntrl}} misses \x80-\x9F. */
    private static final Pattern NO_CONTROL_CHARACTERS = Pattern.compile("[^\\x00-\\x1F\\x7F-\\x9F]+");

    /** Core: {@code @Size(max = 1000)} on {@code NewDeviceRequestRepresentation.id}. */
    private static final int MAX_DEVICE_ID_LENGTH = 1000;

    /** Core: {@code DeviceRequestValidator.PROHIBITED_IDS}. */
    private static final List<String> PROHIBITED_DEVICE_IDS = List.of(".", "..");

    @Autowired
    private DeviceCredentialsApi deviceCredentialsApi;

    @Autowired
    private RestConnector restConnector;

    @Autowired
    private UserApi userApi;

    @Autowired
    private LpwanUserPasswordService lpwanUserPasswordService;

    @Autowired
    private MicroserviceSubscriptionsService subscriptionsService;

    /**
     * Returns the credentials for the given device id, provisioning the account on first use and reusing
     * the stored ones after that.
     *
     * @param deviceId permanent identity of the account; changing it provisions a second one. Short
     *                 enough for the option key that stores the credentials - around 230 characters -
     *                 not {@code .} or {@code ..}, and free of whitespace, slashes, backslashes,
     *                 control characters and {@code + $ :}
     * @throws LpwanDeviceUserCredentialsLostException if the account exists but its password does not
     * @throws IllegalArgumentException                if the device id breaks one of those rules
     */
    public DeviceUserCredentials getOrProvision(final String deviceId) throws LpwanDeviceUserCredentialsLostException {
        validateDeviceId(deviceId);
        Optional<StoredDeviceUser> stored = lpwanUserPasswordService.getDeviceUser(deviceId);
        if (stored.isPresent()) {
            String userName = stored.get().userName();
            if (stored.get().password() == null) {
                throw credentialsLost(userName);
            }
            if (userExists(userName)) {
                return new DeviceUserCredentials(userName, stored.get().password());
            }
            log.info("The provisioned user {} no longer exists in this tenant, provisioning it again", userName);
        } else {
            String expectedResetOnUser = DEVICE_USER_NAME_PREFIX.concat(deviceId);
            if (userExists(expectedResetOnUser)) {
                throw credentialsLost(expectedResetOnUser);
            }
        }

        log.info("Provisioning the account for device id {} through the device registration flow", deviceId);
        DeviceCredentialsRepresentation issued = provision(deviceId);
        log.info("The platform issued the account {} for device id {}", issued.getUsername(), deviceId);
        try {
            lpwanUserPasswordService.saveDeviceUser(deviceId, issued.getUsername(), issued.getPassword());
        } catch (Exception e) {
            discardUser(issued.getUsername(), e);
            throw e;
        }
        return new DeviceUserCredentials(issued.getUsername(), issued.getPassword());
    }

    private void validateDeviceId(String deviceId) {
        int maxLength = maxDeviceIdLength();
        if (deviceId == null
                || deviceId.length() > maxLength
                || !VALID_USERNAME_CHARACTERS.matcher(deviceId).matches()
                || !NO_CONTROL_CHARACTERS.matcher(deviceId).matches()
                || PROHIBITED_DEVICE_IDS.contains(deviceId)) {
            throw new IllegalArgumentException(String.format(
                    "'%s' cannot be used as a device id. It becomes the account's user name and part of a "
                            + "tenant option key, so it must not be empty, must be at most %d characters, "
                            + "must not be '.' or '..', and must contain no whitespace, no slash, no "
                            + "backslash, no control character and none of + $ :",
                    deviceId, maxLength));
        }
    }

    /**
     * The tighter of the two limits the id has to satisfy. The platform allows more in a device request
     * than the option key storing the credentials does, and exceeding the latter would let an id register
     * successfully and then fail on save, which reads as a storage fault rather than a bad input.
     */
    private int maxDeviceIdLength() {
        return Math.min(MAX_DEVICE_ID_LENGTH, lpwanUserPasswordService.maxDeviceIdLength());
    }

    private void discardUser(String userName, Exception cause) {
        try {
            userApi.delete(subscriptionsService.getTenant(), userName);
            log.warn("Could not store the credentials of {}, so the account was removed again. The next "
                    + "attempt provisions it from scratch.", userName, cause);
        } catch (Exception e) {
            log.error("Could not store the credentials of the freshly provisioned account {}, and removing "
                    + "that account again failed too. Delete it by hand, otherwise every further attempt "
                    + "refuses to provision because it cannot tell it apart from an account still in use.",
                    userName, e);
        }
    }

    private LpwanDeviceUserCredentialsLostException credentialsLost(String userName) {
        return new LpwanDeviceUserCredentialsLostException(String.format(
                "The account %s exists but its password is not stored, and the platform cannot be asked "
                        + "for it again. Provisioning it again would reset the password and break the network "
                        + "servers still configured with the old one. Restore the stored credentials, or delete "
                        + "the account so that it is provisioned from scratch and every output re-pushed.",
                userName));
    }

    /** The user name already provisioned for the given device id, without provisioning anything. */
    public Optional<String> getProvisionedUserName(String deviceId) {
        return lpwanUserPasswordService.getDeviceUser(deviceId).map(StoredDeviceUser::userName);
    }

    /**
     * Only a request in PENDING_ACCEPTANCE can be accepted, and the platform moves it there when the
     * device first asks for credentials. So the first collect is what makes the accept possible; an
     * already accepted request returns its credentials there and needs no accept.
     */
    private DeviceCredentialsRepresentation provision(String deviceId) {
        NewDeviceRequestRepresentation request = register(deviceId);

        Optional<DeviceCredentialsRepresentation> alreadyAccepted = tryCollectCredentials(deviceId);
        if (alreadyAccepted.isPresent()) {
            removeRegistrationRecord(deviceId, request);
            return alreadyAccepted.get();
        }

        accept(deviceId);
        DeviceCredentialsRepresentation credentials = deviceCredentialsApi.pollCredentials(deviceId);
        removeRegistrationRecord(deviceId, request);
        return credentials;
    }

    private Optional<DeviceCredentialsRepresentation> tryCollectCredentials(String deviceId) {
        try {
            return Optional.of(deviceCredentialsApi.pollCredentials(deviceId));
        } catch (SDKException e) {
            if (e.getHttpStatus() != SC_NOT_FOUND) {
                log.error("Could not ask for the credentials of {}. The agent's service user needs "
                        + "ROLE_DEVICE_BOOTSTRAP, which is granted from the microservice manifest.", deviceId);
                throw e;
            }
            // Expected while the request is not accepted yet, and it is this attempt that moves the
            // request from WAITING_FOR_CONNECTION to PENDING_ACCEPTANCE.
            log.debug("Credentials for {} are not available yet, the device request now awaits acceptance", deviceId, e);
            return Optional.empty();
        }
    }

    /**
     * A duplicate id is a 422, not a 409: core maps {@code NonUniqueResultException} to
     * {@code CumulocityStatus.NON_UNIQUE_RESULT}. A record left by an interrupted attempt is reused.
     */
    private NewDeviceRequestRepresentation register(String deviceId) {
        try {
            return deviceCredentialsApi.register(deviceId);
        } catch (SDKException e) {
            if (e.getHttpStatus() != SC_UNPROCESSABLE_ENTITY) {
                log.error("Could not create the device request for {}", deviceId);
                throw e;
            }
            log.debug("Device request {} already exists, continuing with the existing one", deviceId, e);
            return restConnector.get(String.format(NEW_DEVICE_REQUEST_PATH, deviceId),
                    DeviceControlMediaType.NEW_DEVICE_REQUEST, NewDeviceRequestRepresentation.class);
        }
    }

    private void accept(String deviceId) {
        NewDeviceRequestRepresentation accepted = new NewDeviceRequestRepresentation();
        accepted.setStatus(ACCEPTED_STATUS);
        restConnector.put(String.format(NEW_DEVICE_REQUEST_PATH, deviceId),
                DeviceControlMediaType.NEW_DEVICE_REQUEST, accepted);
    }

    private void removeRegistrationRecord(String deviceId, NewDeviceRequestRepresentation request) {
        try {
            deviceCredentialsApi.delete(request);
        } catch (Exception e) {
            log.warn("Could not remove the device registration record for {}", deviceId, e);
        }
    }

    /**
     * Only a 404 means the account is not there. Reading any other failure as "does not exist" would let
     * a transient problem re-provision, resetting the password of a working account.
     */
    private boolean userExists(String userName) {
        try {
            return userApi.getUser(subscriptionsService.getTenant(), userName) != null;
        } catch (SDKException e) {
            if (e.getHttpStatus() != SC_NOT_FOUND) {
                log.error("Could not check whether the user {} exists", userName, e);
                throw e;
            }
            log.debug("User {} does not exist", userName, e);
            return false;
        }
    }

    /** Credentials of a provisioned device user. */
    public record DeviceUserCredentials(String userName, String password) {

        /** Overridden so the generated one cannot put the password into a log line. */
        @Override
        public String toString() {
            return "DeviceUserCredentials[userName=" + userName + ", password=***]";
        }
    }
}
