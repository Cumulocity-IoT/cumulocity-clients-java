package com.cumulocity.lpwan.platform.service;

import com.cumulocity.lpwan.lns.connection.model.LnsConnectionDeserializer;
import com.cumulocity.lpwan.platform.service.LpwanUserPasswordService.StoredDeviceUser;
import com.cumulocity.lpwan.sample.connection.model.SampleConnection;
import com.cumulocity.lpwan.util.ImmediateMicroserviceSubscriptionsService;
import com.cumulocity.model.option.OptionPK;
import com.cumulocity.rest.representation.tenant.OptionRepresentation;
import com.cumulocity.sdk.client.SDKException;
import com.cumulocity.sdk.client.option.TenantOptionApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Reading the account an earlier version provisioned through the platform's device registration flow. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LpwanDeviceUserStorageTest {

    private static final String APP_NAME = "loriot";
    private static final String CATEGORY = "loriot";
    private static final String DEVICE_ID = "loriot-agent-output";
    private static final String DEVICE_USER_KEY = "credentials.loriot.device.loriot-agent-output";
    private static final String USER_NAME = "device_loriot-agent-output";
    private static final String PASSWORD = "issued-by-the-platform";

    @Mock
    private TenantOptionApi tenantOptionApi;

    private LpwanUserPasswordService lpwanUserPasswordService;

    @BeforeEach
    void setUp() {
        lpwanUserPasswordService = new LpwanUserPasswordService(tenantOptionApi,
                new ImmediateMicroserviceSubscriptionsService("tenant"));
        lpwanUserPasswordService.setAppName(APP_NAME);
        LnsConnectionDeserializer.registerLnsConnectionConcreteClass("Loriot", SampleConnection.class);
    }

    /**
     * The category decides whether the platform hands the value back decrypted, and it is not always the
     * application name - so the caller's category is what the read uses.
     */
    @Test
    void shouldReadUnderTheCategoryTheCallerGives() {
        when(tenantOptionApi.getOption(new OptionPK("a-different-category", DEVICE_USER_KEY)))
                .thenReturn(OptionRepresentation.asOptionRepresentation("a-different-category", DEVICE_USER_KEY,
                        "{\"username\":\"" + USER_NAME + "\",\"password\":\"" + PASSWORD + "\"}"));

        assertThat(lpwanUserPasswordService.getDeviceUser("a-different-category", DEVICE_ID))
                .map(StoredDeviceUser::userName).contains(USER_NAME);
    }

    /** Keyed by device id under {@code credentials.}, which is what makes the platform encrypt it. */
    @Test
    void shouldReadTheKeyScopedToTheDeviceId() {
        stubStoredValue("{\"username\":\"" + USER_NAME + "\",\"password\":\"" + PASSWORD + "\"}");

        lpwanUserPasswordService.getDeviceUser(CATEGORY, DEVICE_ID);

        verify(tenantOptionApi).getOption(new OptionPK(APP_NAME, DEVICE_USER_KEY));
    }

    /**
     * What the platform substitutes when the category is not ours to decrypt. Reading it as absent sent
     * the caller into "the account exists but its password is lost", which named neither cause nor cure.
     */
    @Test
    void shouldFailLoudlyWhenThePlatformWillNotDecryptTheCredentials() {
        when(tenantOptionApi.getOption(new OptionPK(APP_NAME, DEVICE_USER_KEY)))
                .thenReturn(OptionRepresentation.asOptionRepresentation(APP_NAME, DEVICE_USER_KEY, "<<Encrypted>>"));

        assertThatThrownBy(() -> lpwanUserPasswordService.getDeviceUser(CATEGORY, DEVICE_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("<<Encrypted>>")
                .hasMessageContaining("context path");
    }

    /**
     * Omitting the category cannot compile, so only one that names nothing is left to guard against - nothing
     * could ever be read from it.
     */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldRefuseACategoryThatNamesNothing(String category) {
        assertThatThrownBy(() -> lpwanUserPasswordService.getDeviceUser(category, DEVICE_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("context path");
    }

    /** A record's generated toString would print the password; it must not reach a log line. */
    @Test
    void shouldKeepThePasswordOutOfToString() {
        StoredDeviceUser deviceUser = new StoredDeviceUser(USER_NAME, PASSWORD);

        assertThat(deviceUser.toString())
                .contains(USER_NAME)
                .doesNotContain(PASSWORD);
    }

    /** Pins the stored format 6.0.x wrote: user name and password in one option, under these property names. */
    @Test
    void shouldReadTheStoredFormat() {
        stubStoredValue("{\"username\":\"" + USER_NAME + "\",\"password\":\"" + PASSWORD + "\"}");

        Optional<StoredDeviceUser> deviceUser = lpwanUserPasswordService.getDeviceUser(CATEGORY, DEVICE_ID);

        assertThat(deviceUser).isPresent();
        assertThat(deviceUser.get().userName()).isEqualTo(USER_NAME);
        assertThat(deviceUser.get().password()).isEqualTo(PASSWORD);
    }

    @Test
    void shouldReportNoAccountWhenTheOptionDoesNotExist() {
        when(tenantOptionApi.getOption(any(OptionPK.class))).thenThrow(new SDKException(404, "not found"));

        assertThat(lpwanUserPasswordService.getDeviceUser(CATEGORY, DEVICE_ID)).isEmpty();
    }

    /** Anything other than "not there" is a real problem and must not read as "never provisioned". */
    @Test
    void shouldNotSwallowAFailureOtherThanNotFound() {
        when(tenantOptionApi.getOption(any(OptionPK.class))).thenThrow(new SDKException(403, "forbidden"));

        assertThatThrownBy(() -> lpwanUserPasswordService.getDeviceUser(CATEGORY, DEVICE_ID))
                .isInstanceOf(SDKException.class);
    }

    /**
     * Absent means "no such account", and a value we cannot parse is no evidence of that - it may name an
     * account in use.
     */
    @Test
    void shouldRefuseToReadAnUnparseableValueAsAbsent() {
        stubStoredValue("not json at all");

        assertThatThrownBy(() -> lpwanUserPasswordService.getDeviceUser(CATEGORY, DEVICE_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("could not be read");
    }

    /** The caller must be able to tell "password lost" from "no account". */
    @Test
    void shouldReportAnAccountWhosePasswordIsMissing() {
        stubStoredValue("{\"username\":\"" + USER_NAME + "\"}");

        Optional<StoredDeviceUser> deviceUser = lpwanUserPasswordService.getDeviceUser(CATEGORY, DEVICE_ID);

        assertThat(deviceUser).isPresent();
        assertThat(deviceUser.get().userName()).isEqualTo(USER_NAME);
        assertThat(deviceUser.get().password()).isNull();
    }

    /** Same reasoning: something is stored, so absent would be a lie. */
    @Test
    void shouldRefuseToReadAValueWithoutAUserNameAsAbsent() {
        stubStoredValue("{\"password\":\"" + PASSWORD + "\"}");

        assertThatThrownBy(() -> lpwanUserPasswordService.getDeviceUser(CATEGORY, DEVICE_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("do not name an account");
    }

    @Test
    void shouldTreatAnEmptyValueAsAbsent() {
        stubStoredValue("");

        assertThat(lpwanUserPasswordService.getDeviceUser(CATEGORY, DEVICE_ID)).isEmpty();
    }

    /** A field added by a later version must not make the whole account unreadable. */
    @Test
    void shouldIgnoreUnknownFields() {
        stubStoredValue("{\"username\":\"" + USER_NAME + "\",\"password\":\"" + PASSWORD + "\",\"issuedAt\":\"2026-07-31\"}");

        Optional<StoredDeviceUser> deviceUser = lpwanUserPasswordService.getDeviceUser(CATEGORY, DEVICE_ID);

        assertThat(deviceUser).isPresent();
        assertThat(deviceUser.get().userName()).isEqualTo(USER_NAME);
    }

    private void stubStoredValue(String value) {
        when(tenantOptionApi.getOption(new OptionPK(APP_NAME, DEVICE_USER_KEY)))
                .thenReturn(OptionRepresentation.asOptionRepresentation(APP_NAME, DEVICE_USER_KEY, value));
    }
}
