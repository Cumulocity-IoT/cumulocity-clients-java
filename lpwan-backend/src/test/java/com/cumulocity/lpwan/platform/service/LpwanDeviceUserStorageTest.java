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
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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

/**
 * Kept apart from {@link LpwanUserPasswordServiceTest}, which is JUnit 4 and does not run - the vintage
 * engine is excluded in this module's pom.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LpwanDeviceUserStorageTest {

    private static final String APP_NAME = "loriot";
    private static final String DEVICE_ID = "loriot-agent-output";
    private static final String DEVICE_USER_KEY = "credentials.loriot.device.loriot-agent-output";
    private static final String USER_NAME = "device_loriot-agent-output";
    private static final String PASSWORD = "issued-by-the-platform";

    @Mock
    private TenantOptionApi tenantOptionApi;

    @Captor
    private ArgumentCaptor<OptionRepresentation> optionCaptor;

    private LpwanUserPasswordService lpwanUserPasswordService;

    @BeforeEach
    void setUp() {
        lpwanUserPasswordService = new LpwanUserPasswordService(tenantOptionApi,
                new ImmediateMicroserviceSubscriptionsService("tenant"));
        lpwanUserPasswordService.setAppName(APP_NAME);
        LnsConnectionDeserializer.registerLnsConnectionConcreteClass("Loriot", SampleConnection.class);
    }

    /**
     * The real budget, so the value the service test stubs cannot drift from it. A device id that just
     * fits must produce a key at core's 256 character limit.
     */
    @Test
    void shouldReportHowMuchOfTheOptionKeyIsLeftForTheDeviceId() {
        int maxDeviceIdLength = lpwanUserPasswordService.maxDeviceIdLength();

        assertThat(maxDeviceIdLength).isEqualTo(256 - "credentials.loriot.device.".length());
        assertThat(DEVICE_USER_KEY.length() - DEVICE_ID.length() + maxDeviceIdLength).isEqualTo(256);
    }

    /** The {@code credentials.} prefix is what makes the platform encrypt the value. */
    @Test
    void shouldStoreUnderACredentialsKeyThatIsScopedToTheDeviceId() {
        lpwanUserPasswordService.saveDeviceUser(DEVICE_ID, USER_NAME, PASSWORD);

        verify(tenantOptionApi).save(optionCaptor.capture());
        assertThat(optionCaptor.getValue().getCategory()).isEqualTo(APP_NAME);
        assertThat(optionCaptor.getValue().getKey()).isEqualTo(DEVICE_USER_KEY);
    }

    /** Two device ids must not overwrite each other's account. */
    @Test
    void shouldKeepAccountsOfDifferentDeviceIdsApart() {
        lpwanUserPasswordService.saveDeviceUser("first", USER_NAME, PASSWORD);
        lpwanUserPasswordService.saveDeviceUser("second", USER_NAME, PASSWORD);

        verify(tenantOptionApi, org.mockito.Mockito.times(2)).save(optionCaptor.capture());
        assertThat(optionCaptor.getAllValues())
                .extracting(OptionRepresentation::getKey)
                .containsExactly("credentials.loriot.device.first", "credentials.loriot.device.second");
    }

    /** One option, so that a user name can never be stored without its password or the other way round. */
    @Test
    void shouldStoreUserNameAndPasswordInASingleOption() {
        lpwanUserPasswordService.saveDeviceUser(DEVICE_ID, USER_NAME, PASSWORD);

        verify(tenantOptionApi).save(optionCaptor.capture());
        assertThat(optionCaptor.getValue().getValue())
                .contains(USER_NAME)
                .contains(PASSWORD);
    }

    /** Pins the stored format: renaming a record component would make stored accounts unreadable. */
    @Test
    void shouldStoreTheDocumentedPropertyNames() {
        lpwanUserPasswordService.saveDeviceUser(DEVICE_ID, USER_NAME, PASSWORD);

        verify(tenantOptionApi).save(optionCaptor.capture());
        assertThat(optionCaptor.getValue().getValue())
                .isEqualTo("{\"username\":\"" + USER_NAME + "\",\"password\":\"" + PASSWORD + "\"}");
    }

    /** A record's generated toString would print the password; it must not reach a log line. */
    @Test
    void shouldKeepThePasswordOutOfToString() {
        StoredDeviceUser deviceUser = new StoredDeviceUser(USER_NAME, PASSWORD);

        assertThat(deviceUser.toString())
                .contains(USER_NAME)
                .doesNotContain(PASSWORD);
    }

    @Test
    void shouldReadBackWhatWasStored() {
        lpwanUserPasswordService.saveDeviceUser(DEVICE_ID, USER_NAME, PASSWORD);
        verify(tenantOptionApi).save(optionCaptor.capture());
        stubStoredValue(optionCaptor.getValue().getValue());

        Optional<StoredDeviceUser> deviceUser = lpwanUserPasswordService.getDeviceUser(DEVICE_ID);

        assertThat(deviceUser).isPresent();
        assertThat(deviceUser.get().userName()).isEqualTo(USER_NAME);
        assertThat(deviceUser.get().password()).isEqualTo(PASSWORD);
    }

    @Test
    void shouldReportNoAccountWhenTheOptionDoesNotExist() {
        when(tenantOptionApi.getOption(any(OptionPK.class))).thenThrow(new SDKException(404, "not found"));

        assertThat(lpwanUserPasswordService.getDeviceUser(DEVICE_ID)).isEmpty();
    }

    /** Anything other than "not there" is a real problem and must not read as "never provisioned". */
    @Test
    void shouldNotSwallowAFailureOtherThanNotFound() {
        when(tenantOptionApi.getOption(any(OptionPK.class))).thenThrow(new SDKException(403, "forbidden"));

        assertThatThrownBy(() -> lpwanUserPasswordService.getDeviceUser(DEVICE_ID))
                .isInstanceOf(SDKException.class);
    }

    /** An unreadable value is reported as absent so that the account is re-provisioned and recovers. */
    @Test
    void shouldTreatAnUnreadableValueAsAbsent() {
        stubStoredValue("not json at all");

        assertThat(lpwanUserPasswordService.getDeviceUser(DEVICE_ID)).isEmpty();
    }

    /** The caller must be able to tell "password lost" from "no account" - only the latter may be re-provisioned. */
    @Test
    void shouldReportAnAccountWhosePasswordIsMissing() {
        stubStoredValue("{\"username\":\"" + USER_NAME + "\"}");

        Optional<StoredDeviceUser> deviceUser = lpwanUserPasswordService.getDeviceUser(DEVICE_ID);

        assertThat(deviceUser).isPresent();
        assertThat(deviceUser.get().userName()).isEqualTo(USER_NAME);
        assertThat(deviceUser.get().password()).isNull();
    }

    /** Without a user name there is nothing to protect, so it reads as absent and is re-provisioned. */
    @Test
    void shouldTreatAValueWithoutAUserNameAsAbsent() {
        stubStoredValue("{\"password\":\"" + PASSWORD + "\"}");

        assertThat(lpwanUserPasswordService.getDeviceUser(DEVICE_ID)).isEmpty();
    }

    @Test
    void shouldTreatAnEmptyValueAsAbsent() {
        stubStoredValue("");

        assertThat(lpwanUserPasswordService.getDeviceUser(DEVICE_ID)).isEmpty();
    }

    /** A field added by a later version must not make the whole account unreadable. */
    @Test
    void shouldIgnoreUnknownFields() {
        stubStoredValue("{\"username\":\"" + USER_NAME + "\",\"password\":\"" + PASSWORD + "\",\"issuedAt\":\"2026-07-31\"}");

        Optional<StoredDeviceUser> deviceUser = lpwanUserPasswordService.getDeviceUser(DEVICE_ID);

        assertThat(deviceUser).isPresent();
        assertThat(deviceUser.get().userName()).isEqualTo(USER_NAME);
    }

    private void stubStoredValue(String value) {
        when(tenantOptionApi.getOption(new OptionPK(APP_NAME, DEVICE_USER_KEY)))
                .thenReturn(OptionRepresentation.asOptionRepresentation(APP_NAME, DEVICE_USER_KEY, value));
    }
}
