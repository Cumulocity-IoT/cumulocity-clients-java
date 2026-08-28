package com.cumulocity.lpwan.platform.service;

import com.cumulocity.lpwan.exception.LpwanDeviceUserCredentialsLostException;
import com.cumulocity.lpwan.platform.service.LpwanUserPasswordService.StoredDeviceUser;
import com.cumulocity.microservice.subscription.service.MicroserviceSubscriptionsService;
import com.cumulocity.rest.representation.devicebootstrap.DeviceCredentialsRepresentation;
import com.cumulocity.rest.representation.devicebootstrap.NewDeviceRequestRepresentation;
import com.cumulocity.rest.representation.operation.DeviceControlMediaType;
import com.cumulocity.rest.representation.user.UserRepresentation;
import com.cumulocity.sdk.client.RestConnector;
import com.cumulocity.sdk.client.SDKException;
import com.cumulocity.sdk.client.devicecontrol.DeviceCredentialsApi;
import com.cumulocity.sdk.client.user.UserApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LpwanDeviceUserServiceTest {

    private static final String TENANT_ID = "t12345";
    private static final String DEVICE_ID = "loriot-agent-output";
    private static final String USER_NAME = "device_loriot-agent-output";
    private static final String GENERATED_PASSWORD = "issued-by-the-platform";
    private static final String REQUEST_PATH = "/devicecontrol/newDeviceRequests/" + DEVICE_ID;

    @Mock
    private DeviceCredentialsApi deviceCredentialsApi;

    @Mock
    private RestConnector restConnector;

    @Mock
    private UserApi userApi;

    @Mock
    private LpwanUserPasswordService lpwanUserPasswordService;

    @Mock
    private MicroserviceSubscriptionsService subscriptionsService;

    @InjectMocks
    private LpwanDeviceUserService deviceUserService;

    @Captor
    private ArgumentCaptor<NewDeviceRequestRepresentation> requestCaptor;

    /** 256 minus "credentials.loriot.device." - what LpwanUserPasswordService reports for real. */
    private static final int MAX_DEVICE_ID_LENGTH = 230;

    @BeforeEach
    void setUp() {
        when(subscriptionsService.getTenant()).thenReturn(TENANT_ID);
        when(lpwanUserPasswordService.maxDeviceIdLength()).thenReturn(MAX_DEVICE_ID_LENGTH);
    }

    @Test
    void shouldProvisionThroughTheDeviceRegistrationFlowInOrder() throws Exception {
        stubProvisioning();

        LpwanDeviceUserService.DeviceUserCredentials credentials = deviceUserService.getOrProvision(DEVICE_ID);

        assertThat(credentials.userName()).isEqualTo(USER_NAME);
        assertThat(credentials.password()).isEqualTo(GENERATED_PASSWORD);

        InOrder inOrder = inOrder(deviceCredentialsApi, restConnector);
        inOrder.verify(deviceCredentialsApi).register(DEVICE_ID);
        // the request can only be accepted once this first attempt has moved it to PENDING_ACCEPTANCE
        inOrder.verify(deviceCredentialsApi).pollCredentials(DEVICE_ID);
        inOrder.verify(restConnector).put(eq(REQUEST_PATH), eq(DeviceControlMediaType.NEW_DEVICE_REQUEST), any(NewDeviceRequestRepresentation.class));
        inOrder.verify(deviceCredentialsApi).pollCredentials(DEVICE_ID);
    }

    @Test
    void shouldAcceptTheRequestBeforeCollectingCredentials() throws Exception {
        stubProvisioning();

        deviceUserService.getOrProvision(DEVICE_ID);

        verify(restConnector).put(eq(REQUEST_PATH), eq(DeviceControlMediaType.NEW_DEVICE_REQUEST), requestCaptor.capture());
        assertThat(requestCaptor.getValue().getStatus()).isEqualTo("ACCEPTED");
    }

    /** Going through the user API would force an e-mail address on the account, which is the whole point. */
    @Test
    void shouldNeverTouchTheUserApiForCreationOrUpdate() throws Exception {
        stubProvisioning();

        deviceUserService.getOrProvision(DEVICE_ID);

        verify(userApi, never()).create(anyString(), any(UserRepresentation.class));
        verify(userApi, never()).update(anyString(), any(UserRepresentation.class));
    }

    /** The user name the platform reports is stored, not one derived from the device id. */
    @Test
    void shouldStoreTheIssuedUserNameAndPassword() throws Exception {
        stubProvisioning();

        deviceUserService.getOrProvision(DEVICE_ID);

        verify(lpwanUserPasswordService).saveDeviceUser(DEVICE_ID, USER_NAME, GENERATED_PASSWORD);
    }

    /** The user name is whatever the platform says it is, never derived from the device id. */
    @Test
    void shouldReportTheUserNameThePlatformIssuedEvenWhenItIsNotDerivedFromTheDeviceId() throws Exception {
        DeviceCredentialsRepresentation issued = issuedCredentials();
        issued.setUsername("some-other-name");
        when(lpwanUserPasswordService.getDeviceUser(DEVICE_ID)).thenReturn(Optional.empty());
        when(deviceCredentialsApi.register(DEVICE_ID)).thenReturn(newDeviceRequest());
        doThrow(new SDKException(404, "not accepted yet")).doReturn(issued)
                .when(deviceCredentialsApi).pollCredentials(DEVICE_ID);

        LpwanDeviceUserService.DeviceUserCredentials credentials = deviceUserService.getOrProvision(DEVICE_ID);

        assertThat(credentials.userName()).isEqualTo("some-other-name");
        verify(lpwanUserPasswordService).saveDeviceUser(DEVICE_ID, "some-other-name", GENERATED_PASSWORD);
    }

    /** A record's generated toString would print the password; it must not reach a log line. */
    @Test
    void shouldKeepThePasswordOutOfToString() {
        LpwanDeviceUserService.DeviceUserCredentials credentials =
                new LpwanDeviceUserService.DeviceUserCredentials(USER_NAME, GENERATED_PASSWORD);

        assertThat(credentials.toString())
                .contains(USER_NAME)
                .doesNotContain(GENERATED_PASSWORD);
    }

    @Test
    void shouldRemoveTheRegistrationRecordAfterwards() throws Exception {
        NewDeviceRequestRepresentation request = stubProvisioning();

        deviceUserService.getOrProvision(DEVICE_ID);

        verify(deviceCredentialsApi).delete(request);
    }

    @Test
    void shouldReuseStoredCredentialsWhenTheAccountExists() throws Exception {
        stubStoredDeviceUser(USER_NAME, GENERATED_PASSWORD);
        when(userApi.getUser(TENANT_ID, USER_NAME)).thenReturn(new UserRepresentation());

        LpwanDeviceUserService.DeviceUserCredentials credentials = deviceUserService.getOrProvision(DEVICE_ID);

        assertThat(credentials.userName()).isEqualTo(USER_NAME);
        assertThat(credentials.password()).isEqualTo(GENERATED_PASSWORD);
        verify(deviceCredentialsApi, never()).register(anyString());
        verify(deviceCredentialsApi, never()).pollCredentials(anyString());
        verify(lpwanUserPasswordService, never()).saveDeviceUser(anyString(), anyString(), anyString());
    }

    /** The stored name is looked up and handed out verbatim, never rebuilt from the device id. */
    @Test
    void shouldReuseTheStoredUserNameVerbatim() throws Exception {
        stubStoredDeviceUser("device_renamed-by-the-platform", GENERATED_PASSWORD);
        when(userApi.getUser(TENANT_ID, "device_renamed-by-the-platform")).thenReturn(new UserRepresentation());

        LpwanDeviceUserService.DeviceUserCredentials credentials = deviceUserService.getOrProvision(DEVICE_ID);

        assertThat(credentials.userName()).isEqualTo("device_renamed-by-the-platform");
        verify(userApi).getUser(TENANT_ID, "device_renamed-by-the-platform");
        verify(deviceCredentialsApi, never()).register(anyString());
    }

    @Test
    void shouldProvisionWhenNothingIsStoredYet() throws Exception {
        stubProvisioning();

        deviceUserService.getOrProvision(DEVICE_ID);

        // twice: the attempt that moves the request to PENDING_ACCEPTANCE, then the real collect
        verify(deviceCredentialsApi, times(2)).pollCredentials(DEVICE_ID);
        verify(lpwanUserPasswordService).saveDeviceUser(DEVICE_ID, USER_NAME, GENERATED_PASSWORD);
    }

    /** A stored account the tenant no longer has is worthless, so it is provisioned again. */
    @Test
    void shouldReprovisionWhenTheStoredAccountNoLongerExists() throws Exception {
        stubProvisioning();
        stubStoredDeviceUser(USER_NAME, GENERATED_PASSWORD);
        when(userApi.getUser(TENANT_ID, USER_NAME)).thenThrow(new SDKException(404, "not found"));

        deviceUserService.getOrProvision(DEVICE_ID);

        verify(deviceCredentialsApi).register(DEVICE_ID);
    }

    /**
     * Only a 404 says the account is gone. Treating any other failure as "does not exist" would let a
     * transient platform problem re-provision a working account and reset its password, breaking every
     * network server configured with the old one.
     */
    @Test
    void shouldNotReprovisionWhenTheExistenceCheckFailsForAnyOtherReason() {
        stubStoredDeviceUser(USER_NAME, GENERATED_PASSWORD);
        when(userApi.getUser(TENANT_ID, USER_NAME)).thenThrow(new SDKException(500, "boom"));

        assertThatThrownBy(() -> deviceUserService.getOrProvision(DEVICE_ID))
                .isInstanceOf(SDKException.class);

        verify(deviceCredentialsApi, never()).register(anyString());
        verify(lpwanUserPasswordService, never()).saveDeviceUser(anyString(), anyString(), anyString());
    }

    @Test
    void shouldNotReprovisionWhenTheExistenceCheckIsForbidden() {
        stubStoredDeviceUser(USER_NAME, GENERATED_PASSWORD);
        when(userApi.getUser(TENANT_ID, USER_NAME)).thenThrow(new SDKException(403, "forbidden"));

        assertThatThrownBy(() -> deviceUserService.getOrProvision(DEVICE_ID))
                .isInstanceOf(SDKException.class);

        verify(deviceCredentialsApi, never()).register(anyString());
    }

    /**
     * The behaviour of the user API based implementation this replaced: an account whose password is not
     * stored cannot be recovered, and must not be re-provisioned, because that resets it underneath the
     * network servers already using it. Fail loudly instead.
     */
    @Test
    void shouldFailLoudlyWhenTheAccountIsKnownButItsPasswordIsNot() {
        stubStoredDeviceUser(USER_NAME, null);

        assertThatThrownBy(() -> deviceUserService.getOrProvision(DEVICE_ID))
                .isInstanceOf(LpwanDeviceUserCredentialsLostException.class)
                .hasMessageContaining(USER_NAME);

        verify(deviceCredentialsApi, never()).register(anyString());
        verify(lpwanUserPasswordService, never()).saveDeviceUser(anyString(), anyString(), anyString());
    }

    /** Nothing is looked up in that case either - the account is not to be touched at all. */
    @Test
    void shouldNotEvenCheckExistenceWhenThePasswordIsNotStored() {
        stubStoredDeviceUser(USER_NAME, null);

        assertThatThrownBy(() -> deviceUserService.getOrProvision(DEVICE_ID))
                .isInstanceOf(LpwanDeviceUserCredentialsLostException.class);

        verify(userApi, never()).getUser(anyString(), anyString());
    }

    /**
     * Nothing stored, but the account the platform would overwrite is already there - the tenant option
     * was lost. Provisioning would reset that account's password to a fresh random value, so it must not
     * happen: verified against DeviceCredentialsGeneratorImpl.createOrUpdateDeviceUser, which updates an
     * existing device user rather than refusing.
     */
    @Test
    void shouldRefuseToProvisionOverAnAccountThatAlreadyExists() {
        when(lpwanUserPasswordService.getDeviceUser(DEVICE_ID)).thenReturn(Optional.empty());
        when(userApi.getUser(TENANT_ID, USER_NAME)).thenReturn(new UserRepresentation());

        assertThatThrownBy(() -> deviceUserService.getOrProvision(DEVICE_ID))
                .isInstanceOf(LpwanDeviceUserCredentialsLostException.class)
                .hasMessageContaining(USER_NAME);

        verify(deviceCredentialsApi, never()).register(anyString());
        verify(deviceCredentialsApi, never()).pollCredentials(anyString());
        verify(lpwanUserPasswordService, never()).saveDeviceUser(anyString(), anyString(), anyString());
    }

    /** With nothing stored and no such account, the first provisioning must still go ahead. */
    @Test
    void shouldProvisionWhenNeitherTheCredentialsNorTheAccountExist() throws Exception {
        stubProvisioning();
        when(userApi.getUser(TENANT_ID, USER_NAME)).thenThrow(new SDKException(404, "not found"));

        LpwanDeviceUserService.DeviceUserCredentials credentials = deviceUserService.getOrProvision(DEVICE_ID);

        assertThat(credentials.password()).isEqualTo(GENERATED_PASSWORD);
        verify(deviceCredentialsApi).register(DEVICE_ID);
    }

    /** The guard must not turn a platform blip into a refusal to provision either. */
    @Test
    void shouldNotSwallowAFailureWhileCheckingForAnAccountToProtect() {
        when(lpwanUserPasswordService.getDeviceUser(DEVICE_ID)).thenReturn(Optional.empty());
        when(userApi.getUser(TENANT_ID, USER_NAME)).thenThrow(new SDKException(500, "boom"));

        assertThatThrownBy(() -> deviceUserService.getOrProvision(DEVICE_ID))
                .isInstanceOf(SDKException.class);

        verify(deviceCredentialsApi, never()).register(anyString());
    }

    @Test
    void shouldReportTheProvisionedUserNameWithoutProvisioning() throws Exception {
        stubStoredDeviceUser(USER_NAME, GENERATED_PASSWORD);

        assertThat(deviceUserService.getProvisionedUserName(DEVICE_ID)).contains(USER_NAME);
        verify(deviceCredentialsApi, never()).register(anyString());
    }

    @Test
    void shouldReportNoProvisionedUserNameWhenTheAccountWasNeverProvisioned() throws Exception {
        when(lpwanUserPasswordService.getDeviceUser(DEVICE_ID)).thenReturn(Optional.empty());

        assertThat(deviceUserService.getProvisionedUserName(DEVICE_ID)).isEmpty();
    }

    /**
     * 422 is what core answers for a duplicate device request id - NonUniqueResultException becomes
     * ResourceConstraintsException, which carries CumulocityStatus.NON_UNIQUE_RESULT. Not 409.
     */
    @Test
    void shouldCarryOnWhenARegistrationRecordAlreadyExists() throws Exception {
        when(lpwanUserPasswordService.getDeviceUser(DEVICE_ID)).thenReturn(Optional.empty());
        when(deviceCredentialsApi.register(DEVICE_ID)).thenThrow(new SDKException(422, "already exists"));
        when(restConnector.get(eq(REQUEST_PATH), eq(DeviceControlMediaType.NEW_DEVICE_REQUEST), eq(NewDeviceRequestRepresentation.class)))
                .thenReturn(newDeviceRequest());
        when(deviceCredentialsApi.pollCredentials(DEVICE_ID))
                .thenThrow(new SDKException(404, "not accepted yet"))
                .thenReturn(issuedCredentials());

        LpwanDeviceUserService.DeviceUserCredentials credentials = deviceUserService.getOrProvision(DEVICE_ID);

        assertThat(credentials.password()).isEqualTo(GENERATED_PASSWORD);
    }

    @Test
    void shouldStillReturnCredentialsWhenTheRecordCannotBeRemoved() throws Exception {
        NewDeviceRequestRepresentation request = stubProvisioning();
        doThrow(new SDKException(500, "boom")).when(deviceCredentialsApi).delete(request);

        LpwanDeviceUserService.DeviceUserCredentials credentials = deviceUserService.getOrProvision(DEVICE_ID);

        assertThat(credentials.password()).isEqualTo(GENERATED_PASSWORD);
    }

    /** Credentials that could not be stored must not reach a network server - see getOrProvision. */
    @Test
    void shouldNotHandOutCredentialsThatCouldNotBeStored() throws Exception {
        stubProvisioning();
        doThrow(new SDKException(500, "boom")).when(lpwanUserPasswordService).saveDeviceUser(anyString(), anyString(), anyString());

        assertThatThrownBy(() -> deviceUserService.getOrProvision(DEVICE_ID))
                .isInstanceOf(SDKException.class);
    }

    /** Otherwise the account exists with nobody holding its password, and every retry refuses. */
    @Test
    void shouldRemoveTheAccountAgainWhenItsCredentialsCouldNotBeStored() throws Exception {
        stubProvisioning();
        doThrow(new SDKException(500, "boom")).when(lpwanUserPasswordService).saveDeviceUser(anyString(), anyString(), anyString());

        assertThatThrownBy(() -> deviceUserService.getOrProvision(DEVICE_ID))
                .isInstanceOf(SDKException.class);

        verify(userApi).delete(TENANT_ID, USER_NAME);
    }

    /** The original failure is what the caller needs to see, not whatever the cleanup ran into. */
    @Test
    void shouldReportTheSaveFailureEvenWhenTheAccountCannotBeRemovedAgain() throws Exception {
        stubProvisioning();
        doThrow(new SDKException(500, "save failed")).when(lpwanUserPasswordService).saveDeviceUser(anyString(), anyString(), anyString());
        doThrow(new SDKException(403, "delete forbidden")).when(userApi).delete(TENANT_ID, USER_NAME);

        assertThatThrownBy(() -> deviceUserService.getOrProvision(DEVICE_ID))
                .isInstanceOf(SDKException.class)
                .hasMessageContaining("save failed");
    }

    /** A successful provision must not delete the account it just created. */
    @Test
    void shouldNotRemoveTheAccountWhenItsCredentialsWereStored() throws Exception {
        stubProvisioning();

        deviceUserService.getOrProvision(DEVICE_ID);

        verify(userApi, never()).delete(anyString(), anyString());
    }

    /** Checked before anything is registered, rather than failing halfway through the flow. */
    @ParameterizedTest
    @ValueSource(strings = {"", " ", "with space", "with\ttab", "with/slash", "with+plus", "with$dollar",
            "with:colon", ".", ".."})
    void shouldRefuseADeviceIdThePlatformWouldNotAccept(String deviceId) {
        assertThatThrownBy(() -> deviceUserService.getOrProvision(deviceId))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(deviceCredentialsApi, restConnector);
    }

    /** Core's pattern excludes it, but its annotation message does not name it. */
    @Test
    void shouldRefuseADeviceIdContainingABackslash() {
        assertThatThrownBy(() -> deviceUserService.getOrProvision("with\\backslash"))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(deviceCredentialsApi, restConnector);
    }

    /** \x80-\x9F are control characters in UTF that java's \p{Cntrl} does not match. */
    @ParameterizedTest
    @ValueSource(ints = {0x00, 0x1F, 0x7F, 0x85, 0x9F})
    void shouldRefuseADeviceIdContainingAControlCharacter(int controlCharacter) {
        String deviceId = "with" + (char) controlCharacter + "control";

        assertThatThrownBy(() -> deviceUserService.getOrProvision(deviceId))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(deviceCredentialsApi, restConnector);
    }

    /** The rules exclude specific characters, not everything unusual - a valid id must still work. */
    @ParameterizedTest
    @ValueSource(strings = {"loriot-agent-output", "a", "with.dot", "with_underscore", "with-dash",
            "MixedCase123", "with@at", "with#hash"})
    void shouldAcceptADeviceIdThePlatformWouldAccept(String deviceId) throws Exception {
        when(lpwanUserPasswordService.getDeviceUser(deviceId)).thenReturn(Optional.empty());
        when(deviceCredentialsApi.register(deviceId)).thenReturn(newDeviceRequest());
        when(deviceCredentialsApi.pollCredentials(deviceId))
                .thenThrow(new SDKException(404, "not accepted yet"))
                .thenReturn(issuedCredentials());

        assertThat(deviceUserService.getOrProvision(deviceId)).isNotNull();
    }

    /** Exactly at the limit is allowed; one over is not. */
    @Test
    void shouldAcceptADeviceIdAtTheLengthLimit() throws Exception {
        String deviceId = "d".repeat(MAX_DEVICE_ID_LENGTH);
        when(lpwanUserPasswordService.getDeviceUser(deviceId)).thenReturn(Optional.empty());
        when(deviceCredentialsApi.register(deviceId)).thenReturn(newDeviceRequest());
        when(deviceCredentialsApi.pollCredentials(deviceId))
                .thenThrow(new SDKException(404, "not accepted yet"))
                .thenReturn(issuedCredentials());

        assertThat(deviceUserService.getOrProvision(deviceId)).isNotNull();
    }

    @Test
    void shouldRefuseANullDeviceId() {
        assertThatThrownBy(() -> deviceUserService.getOrProvision(null))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(deviceCredentialsApi, restConnector);
    }

    @Test
    void shouldRefuseADeviceIdLongerThanTheOptionKeyCanHold() {
        assertThatThrownBy(() -> deviceUserService.getOrProvision("d".repeat(MAX_DEVICE_ID_LENGTH + 1)))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(deviceCredentialsApi, restConnector);
    }


    /**
     * Core allows 1000 in a device request, the option key that stores the credentials does not. Such an
     * id used to register successfully and then fail on save, looking like a storage fault.
     */
    @Test
    void shouldRefuseADeviceIdThePlatformWouldAcceptButStorageWouldNot() {
        assertThatThrownBy(() -> deviceUserService.getOrProvision("d".repeat(1000)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at most " + MAX_DEVICE_ID_LENGTH);

        verifyNoInteractions(deviceCredentialsApi, restConnector);
    }
    /** A 403 here means the service user lacks ROLE_DEVICE_BOOTSTRAP; it must not look like "not accepted yet". */
    @Test
    void shouldNotHideAFailureOtherThanNotAcceptedYet() throws Exception {
        when(lpwanUserPasswordService.getDeviceUser(DEVICE_ID)).thenReturn(Optional.empty());
        when(deviceCredentialsApi.register(DEVICE_ID)).thenReturn(newDeviceRequest());
        when(deviceCredentialsApi.pollCredentials(DEVICE_ID)).thenThrow(new SDKException(403, "forbidden"));

        assertThatThrownBy(() -> deviceUserService.getOrProvision(DEVICE_ID))
                .isInstanceOf(SDKException.class);

        verify(restConnector, never()).put(anyString(), any(DeviceControlMediaType.class), any(NewDeviceRequestRepresentation.class));
    }

    /** An interrupted earlier attempt can leave the request accepted; then no accept is needed. */
    @Test
    void shouldSkipTheAcceptWhenTheRequestIsAlreadyAccepted() throws Exception {
        NewDeviceRequestRepresentation request = newDeviceRequest();
        when(lpwanUserPasswordService.getDeviceUser(DEVICE_ID)).thenReturn(Optional.empty());
        when(deviceCredentialsApi.register(DEVICE_ID)).thenReturn(request);
        when(deviceCredentialsApi.pollCredentials(DEVICE_ID)).thenReturn(issuedCredentials());

        LpwanDeviceUserService.DeviceUserCredentials credentials = deviceUserService.getOrProvision(DEVICE_ID);

        assertThat(credentials.password()).isEqualTo(GENERATED_PASSWORD);
        verify(restConnector, never()).put(anyString(), any(DeviceControlMediaType.class), any(NewDeviceRequestRepresentation.class));
        verify(deviceCredentialsApi).delete(request);
    }

    private NewDeviceRequestRepresentation stubProvisioning() {
        NewDeviceRequestRepresentation request = newDeviceRequest();
        when(lpwanUserPasswordService.getDeviceUser(DEVICE_ID)).thenReturn(Optional.empty());
        when(deviceCredentialsApi.register(DEVICE_ID)).thenReturn(request);
        when(deviceCredentialsApi.pollCredentials(DEVICE_ID))
                .thenThrow(new SDKException(404, "not accepted yet"))
                .thenReturn(issuedCredentials());
        return request;
    }

    private void stubStoredDeviceUser(String userName, String password) {
        when(lpwanUserPasswordService.getDeviceUser(DEVICE_ID))
                .thenReturn(Optional.of(new StoredDeviceUser(userName, password)));
    }

    private NewDeviceRequestRepresentation newDeviceRequest() {
        NewDeviceRequestRepresentation request = new NewDeviceRequestRepresentation();
        request.setId(DEVICE_ID);
        request.setSelf("http://platform/devicecontrol/newDeviceRequests/" + DEVICE_ID);
        return request;
    }

    private DeviceCredentialsRepresentation issuedCredentials() {
        DeviceCredentialsRepresentation credentials = new DeviceCredentialsRepresentation();
        credentials.setId(DEVICE_ID);
        credentials.setTenantId(TENANT_ID);
        credentials.setUsername(USER_NAME);
        credentials.setPassword(GENERATED_PASSWORD);
        return credentials;
    }
}
