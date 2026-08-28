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
     */
    public void saveDeviceUser(String deviceId, String userName, String password) {
        String value = serialize(new StoredDeviceUser(userName, password));
        options.save(OptionRepresentation.asOptionRepresentation(appName, getDeviceUserKey(deviceId), value));
    }

    /** The account provisioned for the given device id, empty when there is none in this tenant yet. */
    public Optional<StoredDeviceUser> getDeviceUser(String deviceId) {
        OptionRepresentation fetchedOption;
        try {
            fetchedOption = options.getOption(new OptionPK(appName, getDeviceUserKey(deviceId)));
        } catch (SDKException e) {
            if (e.getHttpStatus() == SC_NOT_FOUND) {
                return Optional.empty();
            }
            throw e;
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
     * An unreadable value reads as absent, so the account is re-provisioned and recovers. A value naming
     * an account without a password is returned as it is - the caller must not re-provision that.
     */
    private static Optional<StoredDeviceUser> deserialize(String value) {
        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }
        try {
            StoredDeviceUser deviceUser = JSON_MAPPER.readValue(value, StoredDeviceUser.class);
            if (deviceUser.userName() == null) {
                log.warn("The stored device user does not name an account, treating it as absent");
                return Optional.empty();
            }
            return Optional.of(deviceUser);
        } catch (JsonProcessingException e) {
            log.warn("The stored device user credentials could not be read, treating them as absent", e);
            return Optional.empty();
        }
    }

    private String getUserPasswordKey() {
        String userPasswordKey = "credentials.%s.password";
        return String.format(userPasswordKey, LnsConnectionDeserializer.getRegisteredAgentName().toLowerCase());
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


