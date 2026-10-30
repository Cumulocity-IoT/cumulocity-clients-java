package com.cumulocity.lpwan.platform.service;

import com.cumulocity.lpwan.lns.connection.model.LnsConnectionDeserializer;
import com.cumulocity.microservice.subscription.service.MicroserviceSubscriptionsService;
import com.cumulocity.model.option.OptionPK;
import com.cumulocity.rest.representation.tenant.OptionRepresentation;
import com.cumulocity.sdk.client.SDKException;
import com.cumulocity.sdk.client.option.TenantOptionApi;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

import static org.apache.http.HttpStatus.SC_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Slf4j
public class LpwanUserPasswordService {

    // Jackson 2 defaults: the credentials are persisted as a tenant option and have to stay
    // readable by, and identical to, what earlier SDK versions wrote.
    private static final ObjectMapper JSON_MAPPER = JsonMapper.builderWithJackson2Defaults()
            .enable(MapperFeature.DEFAULT_VIEW_INCLUSION)
            .build();

    /** Core: {@code @Size(max = 256)} on {@code OptionRepresentation.key}. */
    private static final int MAX_OPTION_KEY_LENGTH = 256;

    private static final int PASSWORD_LENGTH = 32;

    private static final List<String> PASSWORD_CHARACTER_CLASSES = List.of(
            "abcdefghijklmnopqrstuvwxyz", "ABCDEFGHIJKLMNOPQRSTUVWXYZ", "0123456789", "!@#$%&*()_+-=[]|.?><");

    private static final String PASSWORD_CHARACTERS = String.join("", PASSWORD_CHARACTER_CLASSES);

    /** Core: {@code OptionEncryptionService} substitutes this for a value it will not decrypt for us. */
    private static final String ENCRYPTED_PLACEHOLDER = "<<Encrypted>>";

    @Setter
    @Value("${application.name}")
    private String appName;

    @NonNull
    private TenantOptionApi options;

    @NonNull
    private final MicroserviceSubscriptionsService subscriptionsService;

    /**
     * @param userName ignored - the password does not depend on it. Kept so that existing callers compile.
     */
    public String generatePasswordAndSave(String userName) {
        String generatedPassword = generatePassword();
        savePassword(generatedPassword);
        return generatedPassword;
    }

    /**
     * A random password, without storing it - see {@link #savePassword(String)}. It contains a lower case
     * letter, an upper case letter, a digit and a special character, so that it passes a tenant's strong
     * password policy. A draw that misses one is drawn again rather than patched, which keeps every
     * character uniformly random.
     */
    public String generatePassword() {
        String password;
        do {
            password = RandomStringUtils.secure().next(PASSWORD_LENGTH, PASSWORD_CHARACTERS);
        } while (!containsEveryCharacterClass(password));
        return password;
    }

    private static boolean containsEveryCharacterClass(String password) {
        return PASSWORD_CHARACTER_CLASSES.stream().allMatch(characterClass -> StringUtils.containsAny(password, characterClass));
    }

    /**
     * Stores the password {@link #get()} returns. Callers treat a stored password as proof that the user
     * exists, so store it only once the user has been created.
     */
    public void savePassword(String password) {
        subscriptionsService.runForTenant(subscriptionsService.getTenant(), () ->
                options.save(OptionRepresentation.asOptionRepresentation(appName, getUserPasswordKey(), password)));
    }

    public Optional<String> get() {
        return subscriptionsService.callForTenant(subscriptionsService.getTenant(), new Callable<Optional<String>>() {
            @Override
            public Optional<String> call() throws Exception {
                try {
                    OptionRepresentation fetchedOption = options.getOption(new OptionPK(appName, getUserPasswordKey()));
                    return Optional.ofNullable(fetchedOption.getValue());
                } catch (Exception e) {
                    if (e instanceof SDKException && ((SDKException) e).getHttpStatus() == SC_NOT_FOUND) {
                        log.info("No {} user credentials are stored yet", LnsConnectionDeserializer.getRegisteredAgentName());
                        return Optional.empty();
                    }
                    throw e;
                }
            }
        });
    }

    /**
     * The password {@link com.cumulocity.lpwan.platform.repository.LpwanRepository#createOrGetUser(String,
     * String, String, String)} stored for the given user, empty when none is stored yet. Each user has its own
     * option, so that an agent can create a successor next to an account it still uses.
     *
     * @param credentialsCategory see {@link #getDeviceUser(String, String)}
     * @throws IllegalStateException if the platform will not decrypt it for this microservice
     */
    public Optional<String> getPassword(String credentialsCategory, String userName) {
        OptionRepresentation fetchedOption;
        try {
            fetchedOption = options.getOption(new OptionPK(validated(credentialsCategory), getUserPasswordKey(userName)));
        } catch (SDKException e) {
            if (e.getHttpStatus() == SC_NOT_FOUND) {
                return Optional.empty();
            }
            throw e;
        }
        if (ENCRYPTED_PLACEHOLDER.equals(fetchedOption.getValue())) {
            throw new IllegalStateException(String.format(
                    "The platform returned %s instead of the stored password of %s, so the category '%s' it is "
                            + "stored under is not this microservice's settings category and it can never be read "
                            + "back. The category has to be the application's context path.",
                    ENCRYPTED_PLACEHOLDER, userName, credentialsCategory));
        }
        return Optional.ofNullable(fetchedOption.getValue()).filter(password -> !password.isEmpty());
    }

    /**
     * Stores the password {@link #getPassword(String, String)} returns. Callers treat a stored password as proof
     * that the user exists, so store it only once the user has been created.
     *
     * @param credentialsCategory see {@link #getDeviceUser(String, String)}
     */
    public void savePassword(String credentialsCategory, String userName, String password) {
        options.save(OptionRepresentation.asOptionRepresentation(validated(credentialsCategory), getUserPasswordKey(userName), password));
    }

    /**
     * Forgets the password {@link #getPassword(String, String)} returns, so that the user can be created from
     * scratch. Nothing stored is not an error.
     *
     * @param credentialsCategory see {@link #getDeviceUser(String, String)}
     */
    public void deletePassword(String credentialsCategory, String userName) {
        deleteIgnoringAbsent(new OptionPK(validated(credentialsCategory), getUserPasswordKey(userName)));
    }

    /**
     * The password earlier versions stored under {@code ${application.name}} - see {@link #get()} - or empty
     * when none is stored or the platform will not decrypt it for this microservice. It does not whenever the
     * application is deployed under a name other than its context path, so such a password is lost to the
     * agent.
     */
    public Optional<String> getLegacyPassword() {
        Optional<String> password = get();
        if (password.isPresent() && ENCRYPTED_PLACEHOLDER.equals(password.get())) {
            log.warn("The {} user password stored under the category '{}' cannot be read back: the platform "
                    + "decrypts it only for the microservice whose settings category matches",
                    LnsConnectionDeserializer.getRegisteredAgentName(), appName);
            return Optional.empty();
        }
        return password.filter(value -> !value.isEmpty());
    }

    private void deleteIgnoringAbsent(OptionPK optionPK) {
        try {
            options.delete(optionPK);
        } catch (SDKException e) {
            if (e.getHttpStatus() != SC_NOT_FOUND) {
                throw e;
            }
        }
    }

    /**
     * The account an earlier version provisioned for the given device id through the platform's device
     * registration flow, empty when there is none in this tenant. Read only: agents keep such an account while
     * it works, and create new ones through
     * {@link com.cumulocity.lpwan.platform.repository.LpwanRepository#createOrGetUser(String, String, String,
     * String)} instead.
     *
     * @param credentialsCategory the tenant option category it is stored under, which has to be the
     *                            microservice's settings category - its application's context path - because
     *                            the platform decrypts a {@code credentials.} option only for the
     *                            microservice whose settings category matches.
     */
    public Optional<StoredDeviceUser> getDeviceUser(String credentialsCategory, String deviceId) {
        OptionRepresentation fetchedOption;
        try {
            fetchedOption = options.getOption(new OptionPK(validated(credentialsCategory), getDeviceUserKey(deviceId)));
        } catch (SDKException e) {
            if (e.getHttpStatus() == SC_NOT_FOUND) {
                return Optional.empty();
            }
            throw e;
        }
        if (ENCRYPTED_PLACEHOLDER.equals(fetchedOption.getValue())) {
            // Reading this as absent would send the caller down the "account exists but its password is
            // lost" path, which describes neither the cause nor the remedy.
            throw new IllegalStateException(String.format(
                    "The platform returned %s instead of the credentials of device id %s, so the category "
                            + "'%s' they are stored under is not this microservice's settings category and "
                            + "they can never be read back. The category has to be the application's "
                            + "context path.",
                    ENCRYPTED_PLACEHOLDER, deviceId, credentialsCategory));
        }
        return deserialize(fetchedOption.getValue());
    }

    /**
     * Only nothing at all reads as absent. A value that is present but unusable throws: callers treat
     * absent as "no such account", and a stored value we cannot parse is no evidence of that - it may name
     * an account in use. A value naming an account without a password is returned as it is.
     */
    private static Optional<StoredDeviceUser> deserialize(String value) {
        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }
        StoredDeviceUser deviceUser;
        try {
            deviceUser = JSON_MAPPER.readValue(value, StoredDeviceUser.class);
        } catch (JacksonException e) {
            throw new IllegalStateException("The stored device user credentials could not be read", e);
        }
        if (deviceUser.userName() == null) {
            throw new IllegalStateException("The stored device user credentials do not name an account");
        }
        return Optional.of(deviceUser);
    }

    private String getUserPasswordKey() {
        String userPasswordKey = "credentials.%s.password";
        return String.format(userPasswordKey, LnsConnectionDeserializer.getRegisteredAgentName().toLowerCase());
    }

    /** One option per user: {@code credentials.<agent>.<user name>.password}. */
    private static String getUserPasswordKey(String userName) {
        if (StringUtils.isBlank(userName)) {
            throw new IllegalArgumentException("A user name is required to store its password");
        }
        String key = String.format("credentials.%s.%s.password",
                LnsConnectionDeserializer.getRegisteredAgentName().toLowerCase(), userName);
        if (key.length() > MAX_OPTION_KEY_LENGTH) {
            throw new IllegalArgumentException(String.format("The user name %s makes the option key longer than %d characters",
                    userName, MAX_OPTION_KEY_LENGTH));
        }
        return key;
    }

    /** A category that names nothing would store where nothing can be read back. */
    private static String validated(String credentialsCategory) {
        if (StringUtils.isBlank(credentialsCategory)) {
            throw new IllegalArgumentException("The credentials category must name the application's context "
                    + "path - the platform decrypts a credentials option only for the microservice whose "
                    + "settings category matches the category it is stored under.");
        }
        return credentialsCategory;
    }

    /** Keyed by device id; {@code credentials.} makes the platform encrypt it. */
    private String getDeviceUserKey(String deviceId) {
        String deviceUserKey = "credentials.%s.device.%s";
        return String.format(deviceUserKey, LnsConnectionDeserializer.getRegisteredAgentName().toLowerCase(), deviceId);
    }

    /**
     * Persisted form of a device user an earlier version provisioned. The {@code @JsonProperty} names are the
     * stored format; renaming a component without them makes every stored account unreadable.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StoredDeviceUser(@JsonProperty("username") String userName,
                                   @JsonProperty("password") String password) {

        /** Overridden so the generated one cannot put the password into a log line. */
        @Override
        public String toString() {
            return "StoredDeviceUser[userName=" + userName + ", password=***]";
        }
    }
}
