package com.cumulocity.lpwan.platform.service;

import com.cumulocity.lpwan.lns.connection.model.LnsConnectionDeserializer;
import com.cumulocity.microservice.subscription.service.MicroserviceSubscriptionsService;
import com.cumulocity.model.option.OptionPK;
import com.cumulocity.rest.representation.tenant.OptionRepresentation;
import com.cumulocity.sdk.client.SDKException;
import com.cumulocity.sdk.client.option.TenantOptionApi;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

import static org.apache.http.HttpStatus.SC_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Slf4j
public class LpwanUserPasswordService {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    /** Core: {@code @Size(max = 256)} on {@code OptionRepresentation.key}. */
    private static final int MAX_OPTION_KEY_LENGTH = 256;

    /** Core: {@code OptionEncryptionService} substitutes this for a value it will not decrypt for us. */
    private static final String ENCRYPTED_PLACEHOLDER = "<<Encrypted>>";

    @Setter
    @Value("${application.name}")
    private String appName;

    @NonNull
    private TenantOptionApi options;

    @NonNull
    private final MicroserviceSubscriptionsService subscriptionsService;

    public String generatePasswordAndSave(String userName) {
        return subscriptionsService.callForTenant(subscriptionsService.getTenant(), new Callable<String>() {
            @Override
            public String call() {
                String generatedPassword = StrongPasswordGenerator.generate(userName.concat(subscriptionsService.getTenant()), 32);
                options.save(OptionRepresentation.asOptionRepresentation(
                        appName,
                        getUserPasswordKey(), generatedPassword));
                return generatedPassword;
            }
        });
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
                        log.error("Error while retrieving the user credentials", e);
                        return Optional.empty();
                    }
                    throw e;
                }
            }
        });
    }

    /**
     * Stores the account issued for the given device id. User name and password go in one option so that
     * neither can be stored without the other, and in a different one from {@link #get()} so both can
     * coexist while an agent migrates.
     *
     * @param credentialsCategory see {@link #getDeviceUser(String, String)}
     */
    public void saveDeviceUser(String credentialsCategory, String deviceId, String userName, String password) {
        String value = serialize(new StoredDeviceUser(userName, password));
        options.save(OptionRepresentation.asOptionRepresentation(
                validated(credentialsCategory), getDeviceUserKey(deviceId), value));
    }

    /**
     * The account provisioned for the given device id, empty when there is none in this tenant yet.
     *
     * @param credentialsCategory the tenant option category to store under, which has to be the
     *                            microservice's settings category - its application's context path - because
     *                            the platform decrypts a {@code credentials.} option only for the
     *                            microservice whose settings category matches. The same value has to be
     *                            passed here and to {@link #saveDeviceUser}, or nothing can be read back.
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

    private static String serialize(StoredDeviceUser deviceUser) {
        try {
            return JSON_MAPPER.writeValueAsString(deviceUser);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize the device user credentials", e);
        }
    }

    /**
     * Only nothing at all reads as absent. A value that is present but unusable throws: callers treat
     * absent as "safe to provision from scratch", and a stored value we cannot parse is no evidence of
     * that - it may name an account in use. A value naming an account without a password is returned as
     * it is; the caller must not re-provision that either.
     */
    private static Optional<StoredDeviceUser> deserialize(String value) {
        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }
        StoredDeviceUser deviceUser;
        try {
            deviceUser = JSON_MAPPER.readValue(value, StoredDeviceUser.class);
        } catch (JsonProcessingException e) {
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

    /** A category that names nothing would store where nothing can be read back. */
    private static String validated(String credentialsCategory) {
        if (StringUtils.isBlank(credentialsCategory)) {
            throw new IllegalArgumentException("The credentials category must name the application's context "
                    + "path - the platform decrypts a credentials option only for the microservice whose "
                    + "settings category matches the category it is stored under.");
        }
        return credentialsCategory;
    }

    /** How many characters of device id the key that stores it has room for. */
    public int maxDeviceIdLength() {
        return MAX_OPTION_KEY_LENGTH - getDeviceUserKey("").length();
    }

    /** Keyed by device id so accounts do not overwrite each other. {@code credentials.} makes the platform encrypt it. */
    private String getDeviceUserKey(String deviceId) {
        String deviceUserKey = "credentials.%s.device.%s";
        return String.format(deviceUserKey, LnsConnectionDeserializer.getRegisteredAgentName().toLowerCase(), deviceId);
    }

    /**
     * Persisted form of a provisioned device user. The {@code @JsonProperty} names are the stored format;
     * renaming a component without them makes every stored account unreadable.
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

    private static class StrongPasswordGenerator {

        public static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
        public static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        public static final String DIGITS = "0123456789";
        public static final String PUNCTUATION = "!@#$%&*()_+-=[]|.?><";
        public static final List<String> REQUIRED_CHARS = Arrays.asList(LOWER, UPPER, DIGITS, PUNCTUATION);

        public static String generate(String seed, int length) {
            if (length < REQUIRED_CHARS.size() + 1) {
                throw new IllegalArgumentException("Cannot generate strong password containing upper, " +
                        "lower letters, digits and special character when password length " +
                        "is less than five characters. ");
            }

            String initPassword = RandomStringUtils.random(length, 0, 0, true, true, null,
                    new SecureRandom(seed.getBytes()));

            StringBuilder password = new StringBuilder(initPassword);
            SecureRandom random = new SecureRandom();

            NextPositionToPlaceChar nextPositionToPlaceChar = new NextPositionToPlaceChar(random, password.length());
            for (String chars : REQUIRED_CHARS) {
                char requiredChar = chars.charAt(random.nextInt(chars.length() - 1));
                password.setCharAt(nextPositionToPlaceChar.get(), requiredChar);
            }
            return new String(password);
        }

        private static class NextPositionToPlaceChar {

            List<Integer> alreadyReplacedPositions = new ArrayList<>();
            SecureRandom random;
            int passwordLength;

            NextPositionToPlaceChar(SecureRandom random, int passwordLength) {
                this.random = random;
                this.passwordLength = passwordLength;
            }

            int get() {
                int indexToPlaceNewChar = random.nextInt(passwordLength - 1);
                while (alreadyReplacedPositions.contains(indexToPlaceNewChar)) {
                    indexToPlaceNewChar = random.nextInt(passwordLength - 1);
                }
                alreadyReplacedPositions.add(indexToPlaceNewChar);
                return indexToPlaceNewChar;
            }
        }
    }

}


