package com.cumulocity.lpwan.platform.service;

import com.cumulocity.lpwan.lns.connection.model.LnsConnectionDeserializer;
import com.cumulocity.lpwan.sample.connection.model.SampleConnection;
import com.cumulocity.lpwan.util.ImmediateMicroserviceSubscriptionsService;
import com.cumulocity.microservice.subscription.service.MicroserviceSubscriptionsService;
import com.cumulocity.model.option.OptionPK;
import com.cumulocity.rest.representation.tenant.OptionRepresentation;
import com.cumulocity.sdk.client.SDKException;
import com.cumulocity.sdk.client.option.TenantOptionApi;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class LpwanUserPasswordServiceTest {

    private static final String APP_NAME = "loriot";

    private static final String LPWAN_USER_PASSWORD_KEY = "credentials.loriot.password";

    @Mock
    private TenantOptionApi tenantOptionApi;

    private MicroserviceSubscriptionsService subscriptionsService = new ImmediateMicroserviceSubscriptionsService("tenant");

    private LpwanUserPasswordService lpwanUserPasswordService;


    @BeforeEach
    public void setup() {
        lpwanUserPasswordService = new LpwanUserPasswordService(tenantOptionApi, subscriptionsService);
        lpwanUserPasswordService.setAppName(APP_NAME);
        LnsConnectionDeserializer.registerLnsConnectionConcreteClass("Loriot", SampleConnection.class);
    }


    @Test
    public void testGeneratePasswordAndSave() {
        String generatedPassword = lpwanUserPasswordService.generatePasswordAndSave("testUserName");
        verify(tenantOptionApi, times(1))
                .save(argThat(
                        object -> {
                            final OptionRepresentation optionRepresentation = object;
                            return (APP_NAME.equals(optionRepresentation.getCategory())
                                    && LPWAN_USER_PASSWORD_KEY.equals(optionRepresentation.getKey())
                                    && generatedPassword.equals(optionRepresentation.getValue()));
                        }
                ));
    }

    @Test
    public void shouldGeneratePasswordsThatPassAStrongPasswordPolicy() {
        for (int i = 0; i < 1000; i++) {
            String password = lpwanUserPasswordService.generatePassword();

            assertEquals(32, password.length());
            assertTrue(password.matches(".*[a-z].*"), password);
            assertTrue(password.matches(".*[A-Z].*"), password);
            assertTrue(password.matches(".*[0-9].*"), password);
            assertTrue(password.matches(".*[!@#$%&*()_+\\-=\\[\\]|.?><].*"), password);
            assertTrue(password.matches("[a-zA-Z0-9!@#$%&*()_+\\-=\\[\\]|.?><]+"), password);
        }
    }

    /** Nothing derived from the user name or the tenant may make two passwords alike. */
    @Test
    public void shouldGenerateADifferentPasswordForTheSameUserEveryTime() {
        Set<String> passwords = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            passwords.add(lpwanUserPasswordService.generatePasswordAndSave("testUserName"));
        }

        assertEquals(100, passwords.size());
    }

    @Test
    @SneakyThrows
    public void testGetPassword() {
        when(tenantOptionApi.getOption(new OptionPK(APP_NAME, LPWAN_USER_PASSWORD_KEY))).then(invocationOnMock -> {
            return OptionRepresentation.asOptionRepresentation(APP_NAME, LPWAN_USER_PASSWORD_KEY, "test");
        });
        Optional<String> password = lpwanUserPasswordService.get();

        verify(tenantOptionApi, times(1))
                .getOption(argThat(
                        object -> {
                            final OptionPK optionPK = object;
                            return (APP_NAME.equals(optionPK.getCategory())
                                    && LPWAN_USER_PASSWORD_KEY.equals(optionPK.getKey())
                                    && password.get().equals("test"));
                        }
                ));
    }

    @Test
    @SneakyThrows
    public void testPasswordNotFound() {
        when(tenantOptionApi.getOption(any(OptionPK.class))).then(invocationOnMock -> {
            throw new SDKException(404, "");
        });
        Optional<String> password = lpwanUserPasswordService.get();
        verify(tenantOptionApi, times(1))
                .getOption(argThat(
                        object -> {
                            final OptionPK optionPK = object;
                            return (APP_NAME.equals(optionPK.getCategory())
                                    && LPWAN_USER_PASSWORD_KEY.equals(optionPK.getKey()));
                        }
                ));
        assertFalse(password.isPresent());
    }

    // --- the password stored per user under the agent's credentials category ---

    private static final String CATEGORY = "loriot";

    private static final String USER = "output_loriot-agent-v2";

    private static final String USER_PASSWORD_KEY = "credentials.loriot.output_loriot-agent-v2.password";

    private static final String DEPLOYED_APP_NAME = "loriot-agent";

    @Test
    public void shouldReadAndWriteThePasswordOfEachUserUnderTheGivenCategory() {
        when(tenantOptionApi.getOption(new OptionPK(CATEGORY, USER_PASSWORD_KEY)))
                .thenReturn(OptionRepresentation.asOptionRepresentation(CATEGORY, USER_PASSWORD_KEY, "stored"));

        lpwanUserPasswordService.savePassword(CATEGORY, USER, "stored");

        assertEquals(Optional.of("stored"), lpwanUserPasswordService.getPassword(CATEGORY, USER));
        verify(tenantOptionApi).save(argThat(option -> CATEGORY.equals(option.getCategory())
                && USER_PASSWORD_KEY.equals(option.getKey()) && "stored".equals(option.getValue())));
    }

    /** Deployed as its context path, an earlier version's password sits under the same category - not the same key. */
    @Test
    public void shouldKeepAUsersPasswordApartFromTheLegacyOne() {
        when(tenantOptionApi.getOption(new OptionPK(CATEGORY, LPWAN_USER_PASSWORD_KEY)))
                .thenReturn(OptionRepresentation.asOptionRepresentation(CATEGORY, LPWAN_USER_PASSWORD_KEY, "legacy"));
        when(tenantOptionApi.getOption(new OptionPK(CATEGORY, USER_PASSWORD_KEY))).thenThrow(new SDKException(404, "not found"));

        assertFalse(lpwanUserPasswordService.getPassword(CATEGORY, USER).isPresent());
    }

    @Test
    public void shouldReadNoPasswordAsAbsent() {
        when(tenantOptionApi.getOption(any(OptionPK.class))).thenThrow(new SDKException(404, "not found"));

        assertFalse(lpwanUserPasswordService.getPassword(CATEGORY, USER).isPresent());
    }

    /** A placeholder taken for the password would be pushed to the network server as the password. */
    @Test
    public void shouldRefuseAPasswordThePlatformWillNotDecrypt() {
        when(tenantOptionApi.getOption(new OptionPK(CATEGORY, USER_PASSWORD_KEY)))
                .thenReturn(OptionRepresentation.asOptionRepresentation(CATEGORY, USER_PASSWORD_KEY, "<<Encrypted>>"));

        assertThrows(IllegalStateException.class, () -> lpwanUserPasswordService.getPassword(CATEGORY, USER));
    }

    @Test
    public void shouldDeleteThePasswordOfAUserAndIgnoreOneThatIsGone() {
        lpwanUserPasswordService.deletePassword(CATEGORY, USER);
        verify(tenantOptionApi).delete(new OptionPK(CATEGORY, USER_PASSWORD_KEY));

        doThrow(new SDKException(404, "not found")).when(tenantOptionApi).delete(any(OptionPK.class));
        lpwanUserPasswordService.deletePassword(CATEGORY, USER);
    }

    @Test
    public void shouldReturnAReadableLegacyPassword() {
        when(tenantOptionApi.getOption(new OptionPK(APP_NAME, LPWAN_USER_PASSWORD_KEY)))
                .thenReturn(OptionRepresentation.asOptionRepresentation(APP_NAME, LPWAN_USER_PASSWORD_KEY, "legacy"));

        assertEquals(Optional.of("legacy"), lpwanUserPasswordService.getLegacyPassword());
    }

    /** Deployed under a name other than the context path, the platform hands back a placeholder. */
    @Test
    public void shouldNotReturnAnUndecryptableLegacyPassword() {
        lpwanUserPasswordService.setAppName(DEPLOYED_APP_NAME);
        when(tenantOptionApi.getOption(new OptionPK(DEPLOYED_APP_NAME, LPWAN_USER_PASSWORD_KEY)))
                .thenReturn(OptionRepresentation.asOptionRepresentation(DEPLOYED_APP_NAME, LPWAN_USER_PASSWORD_KEY, "<<Encrypted>>"));

        assertFalse(lpwanUserPasswordService.getLegacyPassword().isPresent());
    }
}
