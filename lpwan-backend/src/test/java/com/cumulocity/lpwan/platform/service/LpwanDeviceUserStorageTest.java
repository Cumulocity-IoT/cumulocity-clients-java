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
    private static final String CATEGORY = "loriot";
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
        lpwanUserPasswordService.saveDeviceUser(CATEGORY, DEVICE_ID, USER_NAME, PASSWORD);

        verify(tenantOptionApi).save(optionCaptor.capture());
        assertThat(optionCaptor.getValue().getCategory()).isEqualTo(APP_NAME);
        assertThat(optionCaptor.getValue().getKey()).isEqualTo(DEVICE_USER_KEY);
    }

    /**
     * The category decides whether the platform hands the value back decrypted, and it is not always the
     * application name - so the caller's category is what both read and write have to use.
     */
    @Test
    void shouldStoreAndReadUnderTheCategoryTheCallerGives() {
        when(tenantOptionApi.getOption(new OptionPK("a-different-category", DEVICE_USER_KEY)))
                .thenReturn(OptionRepresentation.asOptionRepresentation("a-different-category", DEVICE_USER_KEY,
                        "{\"username\":\"" + USER_NAME + "\",\"password\":\"" + PASSWORD + "\"}"));

        lpwanUserPasswordService.saveDeviceUser("a-different-category", DEVICE_ID, USER_NAME, PASSWORD);

        verify(tenantOptionApi).save(optionCaptor.capture());
        assertThat(optionCaptor.getValue().getCategory()).isEqualTo("a-different-category");
        assertThat(lpwanUserPasswordService.getDeviceUser("a-different-category", DEVICE_ID))
                .map(StoredDeviceUser::userName).contains(USER_NAME);
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
     * Omitting the category cannot compile, so only one that names nothing is left to guard against - it
     * would store where nothing can be read back.
     */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldRefuseACategoryThatNamesNothing(String category) {
        assertThatThrownBy(() -> lpwanUserPasswordService.saveDeviceUser(category, DEVICE_ID, USER_NAME, PASSWORD))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("context path");
        assertThatThrownBy(() -> lpwanUserPasswordService.getDeviceUser(category, DEVICE_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("context path");
    }

    /** Two device ids must not overwrite each other's account. */
    @Test
    void shouldKeepAccountsOfDifferentDeviceIdsApart() {
        lpwanUserPasswordService.saveDeviceUser(CATEGORY, "first", USER_NAME, PASSWORD);
        lpwanUserPasswordService.saveDeviceUser(CATEGORY, "second", USER_NAME, PASSWORD);

        verify(tenantOptionApi, org.mockito.Mockito.times(2)).save(optionCaptor.capture());
        assertThat(optionCaptor.getAllValues())
                .extracting(OptionRepresentation::getKey)
                .containsExactly("credentials.loriot.device.first", "credentials.loriot.device.second");
    }

    /** One option, so that a user name can never be stored without its password or the other way round. */
    @Test
    void shouldStoreUserNameAndPasswordInASingleOption() {
        lpwanUserPasswordService.saveDeviceUser(CATEGORY, DEVICE_ID, USER_NAME, PASSWORD);

        verify(tenantOptionApi).save(optionCaptor.capture());
        assertThat(optionCaptor.getValue().getValue())
                .contains(USER_NAME)
                .contains(PASSWORD);
    }

    /** Pins the stored format: renaming a record component would make stored accounts unreadable. */
    @Test
    void shouldStoreTheDocumentedPropertyNames() {
        lpwanUserPasswordService.saveDeviceUser(CATEGORY, DEVICE_ID, USER_NAME, PASSWORD);

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
        lpwanUserPasswordService.saveDeviceUser(CATEGORY, DEVICE_ID, USER_NAME, PASSWORD);
        verify(tenantOptionApi).save(optionCaptor.capture());
        stubStoredValue(optionCaptor.getValue().getValue());

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
     * Absent means "safe to provision from scratch", and a value we cannot parse is no evidence of that -
     * it may name an account in use, which a caller could then delete or reset.
     */
    @Test
    void shouldRefuseToReadAnUnparseableValueAsAbsent() {
        stubStoredValue("not json at all");

        assertThatThrownBy(() -> lpwanUserPasswordService.getDeviceUser(CATEGORY, DEVICE_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("could not be read");
    }

    /** The caller must be able to tell "password lost" from "no account" - only the latter may be re-provisioned. */
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
