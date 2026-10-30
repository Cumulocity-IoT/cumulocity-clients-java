package com.cumulocity.lpwan.platform.repository;

import com.cumulocity.lpwan.exception.LpwanUserManagementNotPermittedException;
import com.cumulocity.lpwan.exception.LpwanUserNotFoundException;
import com.cumulocity.lpwan.lns.connection.model.LnsConnectionDeserializer;
import com.cumulocity.lpwan.platform.service.LpwanUserPasswordService;
import com.cumulocity.lpwan.sample.connection.model.SampleConnection;
import com.cumulocity.model.ID;
import com.cumulocity.model.idtype.GId;
import com.cumulocity.rest.representation.alarm.AlarmRepresentation;
import com.cumulocity.rest.representation.identity.ExternalIDRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjects;
import com.cumulocity.rest.representation.user.GroupRepresentation;
import com.cumulocity.rest.representation.user.UserMediaType;
import com.cumulocity.rest.representation.user.UserReferenceRepresentation;
import com.cumulocity.rest.representation.user.UserRepresentation;
import com.cumulocity.sdk.client.RestConnector;
import com.cumulocity.sdk.client.SDKException;
import com.cumulocity.sdk.client.alarm.AlarmApi;
import com.cumulocity.sdk.client.alarm.AlarmCollection;
import com.cumulocity.sdk.client.alarm.AlarmFilter;
import com.cumulocity.sdk.client.alarm.PagedAlarmCollectionRepresentation;
import com.cumulocity.sdk.client.identity.IdentityApi;
import com.cumulocity.sdk.client.inventory.InventoryApi;
import com.cumulocity.sdk.client.user.UserApi;
import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Arrays;

import static com.cumulocity.model.event.CumulocityAlarmStatuses.ACTIVE;
import static com.cumulocity.model.event.CumulocityAlarmStatuses.CLEARED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class LpwanRepositoryTest {

    @Mock
    private IdentityApi identity;

    @Mock
    private InventoryApi inventory;

    @Mock
    private AlarmApi alarmApi;
    
    @Mock
    private UserApi userUserApi;

    @Mock
    private RestConnector userRestConnector;
    
    @Mock
    private LpwanUserPasswordService lpwanUserPasswordService;

    @InjectMocks
    private LpwanRepository lpwanRepository;
    
    private static final String OUTPUT_USERNAME = "output_loriot-agent";
    
    private static final String EMAIL_HOST = "loriotuser.com";

    private static final String TENANT = "t12345";

    private static final long DEVICES_GROUP_ID = 12345L;

    private static final String GROUP_BY_NAME_PATH = "/user/" + TENANT + "/groupByName/devices";

    private static final String ADD_TO_GROUP_PATH = "/user/" + TENANT + "/groups/" + DEVICES_GROUP_ID + "/users";

    @BeforeEach
    public void setup() {
        LnsConnectionDeserializer.registerLnsConnectionConcreteClass("Loriot", SampleConnection.class);
    }

    @Test
    public void shouldTestCreateExternalId(){
        lpwanRepository.createExternalId(GId.asGId("12345"),"DummyType", "DummyIdentifier");
        ArgumentCaptor<ExternalIDRepresentation> externalIDRepresentationArgumentCaptor = ArgumentCaptor.forClass(ExternalIDRepresentation.class);
        verify(identity).create(externalIDRepresentationArgumentCaptor.capture());
        ExternalIDRepresentation externalIDRepresentation = externalIDRepresentationArgumentCaptor.getValue();
        assertEquals(GId.asGId("12345"), externalIDRepresentation.getManagedObject().getId());
        assertEquals("DummyIdentifier", externalIDRepresentation.getExternalId());
        assertEquals("DummyType", externalIDRepresentation.getType());
    }

    @Test
    public void shouldTestFindGId(){
        ExternalIDRepresentation externalId = new ExternalIDRepresentation();
        externalId.setManagedObject(ManagedObjects.asManagedObject(GId.asGId("12345")));
        when(identity.getExternalId(any(ID.class))).thenReturn(externalId);
        Optional<GId> gIdOptional = lpwanRepository.findGId("DummyType", "DummyId");
        assertTrue(gIdOptional.isPresent());
        assertEquals(GId.asGId("12345"), gIdOptional.get());
    }

    @Test
    public void shouldThrowExceptionFindGId(){
        when(identity.getExternalId(any(ID.class))).thenThrow(new SDKException(HttpStatus.NOT_FOUND.value(), "NOT FOUND"));
        Optional<GId> gIdOptional = lpwanRepository.findGId("DummyType", "DummyId");
        assertFalse(gIdOptional.isPresent());
    }

    @Test
    public void shouldCreate() {
        GId sourceId = GId.asGId(456);
        when(alarmApi.create(any(AlarmRepresentation.class))).thenReturn(mock(AlarmRepresentation.class));

        lpwanRepository.createAlarm(sourceId, "CRITICAL", "type", "text");

        ArgumentCaptor<AlarmRepresentation> alarmRepresentationCaptor = ArgumentCaptor.forClass(AlarmRepresentation.class);
        verify(alarmApi).create(alarmRepresentationCaptor.capture());
        AlarmRepresentation actualAlarm = alarmRepresentationCaptor.getValue();
        AlarmRepresentation expectedAlarm = representation(sourceId, "CRITICAL", "type", "text", ACTIVE.name());

        assertEquals(expectedAlarm.getSource().getId(), actualAlarm.getSource().getId());
    }

    private AlarmRepresentation representation(GId sourceId, String critical, String type, String text, String status) {
        final ManagedObjectRepresentation source = new ManagedObjectRepresentation();
        source.setId(sourceId);

        final AlarmRepresentation alarm = new AlarmRepresentation();
        alarm.setSource(source);
        alarm.setSeverity(critical);
        alarm.setType(type);
        alarm.setText(text);
        alarm.setDateTime(new DateTime(456546));
        alarm.setStatus(status);
        return alarm;
    }

    @Test
    public void shouldCLearAlarms() {
        GId sourceId = GId.asGId(456);
        String type = "c8y_TestType";

        PagedAlarmCollectionRepresentation alarmRepresentations = mock(PagedAlarmCollectionRepresentation.class);
        AlarmCollection alarmCollection = mock(AlarmCollection.class);

        when(alarmApi.getAlarmsByFilter(any(AlarmFilter.class))).thenReturn(alarmCollection);
        AlarmRepresentation alarmRepresentation = representation(sourceId, "CRITICAL", type, "text", CLEARED.name());
        Iterable<AlarmRepresentation> iterable = Arrays.asList(alarmRepresentation);
        when(alarmCollection.get()).thenReturn(alarmRepresentations);
        when(alarmRepresentations.allPages()).thenReturn(iterable);

        lpwanRepository.clearAlarm(sourceId, type);

        verify(alarmApi).getAlarmsByFilter(any(AlarmFilter.class));
        verify(alarmApi).update(alarmRepresentation);
    }

    @Test
    public void shouldReturnTheStoredCredentialsWithoutCallingTheUserApi() throws LpwanUserNotFoundException {
        when(lpwanUserPasswordService.get()).thenReturn(java.util.Optional.of("Dummy_Password"));

        UserRepresentation user = lpwanRepository.createOrGetUser(TENANT, OUTPUT_USERNAME, EMAIL_HOST);

        assertEquals(OUTPUT_USERNAME, user.getUserName());
        assertEquals("Dummy_Password", user.getPassword());
        assertNull(user.getEmail());
        verifyNoInteractions(userUserApi, userRestConnector);
        verify(lpwanUserPasswordService, never()).savePassword(anyString());
    }

    @Test
    public void shouldCreateTheUserWhenNoCredentialsAreStored() throws LpwanUserNotFoundException {
        stubFirstTimeSetup();
        when(userUserApi.create(eq(TENANT), any(UserRepresentation.class))).thenAnswer(i -> i.getArguments()[1]);
        when(userRestConnector.get(GROUP_BY_NAME_PATH, UserMediaType.GROUP, GroupRepresentation.class)).thenReturn(devicesGroup());

        UserRepresentation user = lpwanRepository.createOrGetUser(TENANT, OUTPUT_USERNAME, EMAIL_HOST);

        assertEquals(OUTPUT_USERNAME.concat("@").concat(EMAIL_HOST), user.getEmail());
        assertEquals("Dummy_Password", user.getPassword());
        InOrder inOrder = inOrder(userUserApi, userRestConnector, lpwanUserPasswordService);
        inOrder.verify(userUserApi).create(eq(TENANT), any(UserRepresentation.class));
        inOrder.verify(userRestConnector).post(eq(ADD_TO_GROUP_PATH), eq(UserMediaType.USER_REFERENCE), any(UserReferenceRepresentation.class));
        // stored last, because a stored password is what marks the user as existing
        inOrder.verify(lpwanUserPasswordService).savePassword("Dummy_Password");
    }

    @Test
    public void shouldExplainThatTheFirstTimeSetupNeedsUserManagementRights() {
        stubFirstTimeSetup();
        when(userUserApi.create(eq(TENANT), any(UserRepresentation.class))).thenThrow(new SDKException(403, "forbidden"));

        LpwanUserManagementNotPermittedException e = assertThrows(LpwanUserManagementNotPermittedException.class,
                () -> lpwanRepository.createOrGetUser(TENANT, OUTPUT_USERNAME, EMAIL_HOST));

        assertEquals("Setting up the Loriot integration for the first time requires a user with user management "
                + "admin rights.", e.getMessage());
        verify(lpwanUserPasswordService, never()).savePassword(anyString());
        verify(userUserApi, never()).delete(anyString(), anyString());
    }

    @Test
    public void shouldRemoveTheUserAgainWhenItCannotBeAddedToTheDevicesGroup() {
        stubFirstTimeSetup();
        when(userUserApi.create(eq(TENANT), any(UserRepresentation.class))).thenAnswer(i -> i.getArguments()[1]);
        when(userRestConnector.get(GROUP_BY_NAME_PATH, UserMediaType.GROUP, GroupRepresentation.class)).thenThrow(new SDKException(403, "forbidden"));

        assertThrows(LpwanUserManagementNotPermittedException.class,
                () -> lpwanRepository.createOrGetUser(TENANT, OUTPUT_USERNAME, EMAIL_HOST));

        verify(userUserApi).delete(TENANT, OUTPUT_USERNAME);
        verify(lpwanUserPasswordService, never()).savePassword(anyString());
    }

    @Test
    public void shouldRemoveTheUserAgainWhenItsPasswordCannotBeStored() {
        stubFirstTimeSetup();
        when(userUserApi.create(eq(TENANT), any(UserRepresentation.class))).thenAnswer(i -> i.getArguments()[1]);
        when(userRestConnector.get(GROUP_BY_NAME_PATH, UserMediaType.GROUP, GroupRepresentation.class)).thenReturn(devicesGroup());
        doThrow(new SDKException(500, "save failed")).when(lpwanUserPasswordService).savePassword("Dummy_Password");

        SDKException e = assertThrows(SDKException.class,
                () -> lpwanRepository.createOrGetUser(TENANT, OUTPUT_USERNAME, EMAIL_HOST));

        assertEquals(500, e.getHttpStatus());
        verify(userUserApi).delete(TENANT, OUTPUT_USERNAME);
    }

    /** The user exists but its password is not stored - it cannot be recovered, as before. */
    @Test
    public void shouldThrowWhenTheUserExistsButItsPasswordIsNotStored() {
        stubFirstTimeSetup();
        when(userUserApi.create(eq(TENANT), any(UserRepresentation.class))).thenThrow(new SDKException(409, "duplicate"));

        assertThrows(LpwanUserNotFoundException.class,
                () -> lpwanRepository.createOrGetUser(TENANT, OUTPUT_USERNAME, EMAIL_HOST));

        verify(lpwanUserPasswordService, never()).savePassword(anyString());
        verify(userUserApi, never()).delete(anyString(), anyString());
    }

    // --- createOrGetUser with a credentials category and a full e-mail address ---

    private static final String CATEGORY = "loriot";

    private static final String EMAIL = "noreply@loriot-agent.invalid";

    @Test
    public void shouldReturnThePasswordStoredUnderTheCategoryWithoutCallingTheUserApi() throws LpwanUserNotFoundException {
        when(lpwanUserPasswordService.getPassword(CATEGORY, OUTPUT_USERNAME)).thenReturn(java.util.Optional.of("Stored_Password"));

        UserRepresentation user = lpwanRepository.createOrGetUser(TENANT, CATEGORY, OUTPUT_USERNAME, EMAIL);

        assertEquals(OUTPUT_USERNAME, user.getUserName());
        assertEquals("Stored_Password", user.getPassword());
        verifyNoInteractions(userUserApi, userRestConnector);
    }

    /**
     * A password an earlier version stored under the application name belongs to the earlier account - taking
     * it for this one would hand a successor its predecessor's password.
     */
    @Test
    public void shouldNeverTakeTheLegacyPasswordForTheAccount() throws LpwanUserNotFoundException {
        stubNothingStored();
        when(lpwanUserPasswordService.getLegacyPassword()).thenReturn(java.util.Optional.of("Legacy_Password"));
        when(userUserApi.create(eq(TENANT), any(UserRepresentation.class))).thenAnswer(i -> i.getArguments()[1]);
        when(userRestConnector.get(GROUP_BY_NAME_PATH, UserMediaType.GROUP, GroupRepresentation.class)).thenReturn(devicesGroup());

        UserRepresentation user = lpwanRepository.createOrGetUser(TENANT, CATEGORY, OUTPUT_USERNAME, EMAIL);

        assertEquals("Dummy_Password", user.getPassword());
        verify(lpwanUserPasswordService, never()).getLegacyPassword();
    }

    /** The given address as it is, and the devices group, as the earlier version did. */
    @Test
    public void shouldCreateTheUserWithTheGivenAddressInTheDevicesGroup() throws LpwanUserNotFoundException {
        stubNothingStored();
        when(userRestConnector.get(GROUP_BY_NAME_PATH, UserMediaType.GROUP, GroupRepresentation.class)).thenReturn(devicesGroup());
        java.util.List<String> idsSentToTheUserApi = new java.util.ArrayList<>();
        when(userUserApi.create(eq(TENANT), any(UserRepresentation.class))).thenAnswer(i -> {
            idsSentToTheUserApi.add(((UserRepresentation) i.getArguments()[1]).getId());
            return i.getArguments()[1];
        });

        UserRepresentation user = lpwanRepository.createOrGetUser(TENANT, CATEGORY, OUTPUT_USERNAME, EMAIL);

        assertEquals(EMAIL, user.getEmail());
        assertEquals("Dummy_Password", user.getPassword());
        // the platform assigns the id and refuses a create that sets one (422 "not updateable: id")
        assertEquals(java.util.Collections.singletonList(null), idsSentToTheUserApi);
        InOrder inOrder = inOrder(userUserApi, userRestConnector, lpwanUserPasswordService);
        inOrder.verify(userUserApi).create(eq(TENANT), any(UserRepresentation.class));
        inOrder.verify(userRestConnector).post(eq(ADD_TO_GROUP_PATH), eq(UserMediaType.USER_REFERENCE), any(UserReferenceRepresentation.class));
        // stored last, because a stored password is what marks the user as existing
        inOrder.verify(lpwanUserPasswordService).savePassword(CATEGORY, OUTPUT_USERNAME, "Dummy_Password");
    }

    @Test
    public void shouldRemoveTheNewUserAgainWhenItCannotBeAddedToTheDevicesGroup() {
        stubNothingStored();
        when(userUserApi.create(eq(TENANT), any(UserRepresentation.class))).thenAnswer(i -> i.getArguments()[1]);
        when(userRestConnector.get(GROUP_BY_NAME_PATH, UserMediaType.GROUP, GroupRepresentation.class)).thenThrow(new SDKException(403, "forbidden"));

        assertThrows(LpwanUserManagementNotPermittedException.class,
                () -> lpwanRepository.createOrGetUser(TENANT, CATEGORY, OUTPUT_USERNAME, EMAIL));

        verify(userUserApi).delete(TENANT, OUTPUT_USERNAME);
        verify(lpwanUserPasswordService, never()).savePassword(anyString(), anyString(), anyString());
    }

    @Test
    public void shouldRemoveTheNewUserAgainWhenItsPasswordCannotBeStoredUnderTheCategory() {
        stubNothingStored();
        when(userUserApi.create(eq(TENANT), any(UserRepresentation.class))).thenAnswer(i -> i.getArguments()[1]);
        when(userRestConnector.get(GROUP_BY_NAME_PATH, UserMediaType.GROUP, GroupRepresentation.class)).thenReturn(devicesGroup());
        doThrow(new SDKException(500, "save failed")).when(lpwanUserPasswordService).savePassword(CATEGORY, OUTPUT_USERNAME, "Dummy_Password");

        assertThrows(SDKException.class, () -> lpwanRepository.createOrGetUser(TENANT, CATEGORY, OUTPUT_USERNAME, EMAIL));

        verify(userUserApi).delete(TENANT, OUTPUT_USERNAME);
    }

    @Test
    public void shouldExplainThatCreatingTheUserNeedsUserManagementRights() {
        stubNothingStored();
        when(userUserApi.create(eq(TENANT), any(UserRepresentation.class))).thenThrow(new SDKException(403, "forbidden"));

        assertThrows(LpwanUserManagementNotPermittedException.class,
                () -> lpwanRepository.createOrGetUser(TENANT, CATEGORY, OUTPUT_USERNAME, EMAIL));

        verify(lpwanUserPasswordService, never()).savePassword(anyString(), anyString(), anyString());
    }

    /** The account - or another one with the address - exists, but no readable password: the caller decides. */
    @Test
    public void shouldReportAnExistingUserWithoutAReadablePassword() {
        stubNothingStored();
        when(userUserApi.create(eq(TENANT), any(UserRepresentation.class))).thenThrow(new SDKException(409, "duplicate"));

        assertThrows(LpwanUserNotFoundException.class,
                () -> lpwanRepository.createOrGetUser(TENANT, CATEGORY, OUTPUT_USERNAME, EMAIL));

        verify(userUserApi, never()).delete(anyString(), anyString());
    }

    @Test
    public void shouldDeleteTheUserAndForgetItsPassword() {
        lpwanRepository.deleteUser(TENANT, CATEGORY, OUTPUT_USERNAME);

        InOrder inOrder = inOrder(userUserApi, lpwanUserPasswordService);
        inOrder.verify(userUserApi).delete(TENANT, OUTPUT_USERNAME);
        inOrder.verify(lpwanUserPasswordService).deletePassword(CATEGORY, OUTPUT_USERNAME);
    }

    @Test
    public void shouldForgetThePasswordOfAUserThatIsAlreadyGone() {
        doThrow(new SDKException(404, "not found")).when(userUserApi).delete(TENANT, OUTPUT_USERNAME);

        lpwanRepository.deleteUser(TENANT, CATEGORY, OUTPUT_USERNAME);

        verify(lpwanUserPasswordService).deletePassword(CATEGORY, OUTPUT_USERNAME);
    }

    @Test
    public void shouldKeepThePasswordWhenTheCallerMayNotDeleteTheUser() {
        doThrow(new SDKException(403, "forbidden")).when(userUserApi).delete(TENANT, OUTPUT_USERNAME);

        assertThrows(LpwanUserManagementNotPermittedException.class,
                () -> lpwanRepository.deleteUser(TENANT, CATEGORY, OUTPUT_USERNAME));

        verify(lpwanUserPasswordService, never()).deletePassword(anyString(), anyString());
    }

    private void stubNothingStored() {
        when(lpwanUserPasswordService.getPassword(CATEGORY, OUTPUT_USERNAME)).thenReturn(java.util.Optional.empty());
        when(lpwanUserPasswordService.generatePassword()).thenReturn("Dummy_Password");
    }

    private void stubFirstTimeSetup() {
        when(lpwanUserPasswordService.get()).thenReturn(java.util.Optional.empty());
        when(lpwanUserPasswordService.generatePassword()).thenReturn("Dummy_Password");
    }

    private static GroupRepresentation devicesGroup() {
        GroupRepresentation group = new GroupRepresentation();
        group.setId(DEVICES_GROUP_ID);
        return group;
    }
}
