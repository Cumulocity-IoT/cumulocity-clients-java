package com.cumulocity.lpwan.lns.connection.manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LnsAuthorizationManagerTest {

    private static final String READ_ROLE = "ROLE_READ";
    private static final String ADMIN_ROLE = "ROLE_ADMIN";
    private static final String USER_ROLE = "ROLE_USER";

    private LnsAuthorizationManager authorizationManager;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        authorizationManager = new TestLnsAuthorizationManager();
        authentication = mock(Authentication.class);
    }

    @SuppressWarnings("unchecked")
    private void mockAuthorities(Collection<? extends GrantedAuthority> authorities) {
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);
    }

    @Test
    void canReadShouldReturnTrueWhenUserHasReadRole() {
        // Given
        mockAuthorities(Collections.singletonList(new SimpleGrantedAuthority(READ_ROLE)));

        // When
        boolean result = authorizationManager.canRead(authentication);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void canReadShouldReturnTrueWhenUserHasAdminRole() {
        // Given
        mockAuthorities(Collections.singletonList(new SimpleGrantedAuthority(ADMIN_ROLE)));

        // When
        boolean result = authorizationManager.canRead(authentication);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void canReadShouldReturnTrueWhenUserHasMultipleRolesIncludingReadRole() {
        // Given
        mockAuthorities(Arrays.asList(
                new SimpleGrantedAuthority(USER_ROLE),
                new SimpleGrantedAuthority(READ_ROLE)
        ));

        // When
        boolean result = authorizationManager.canRead(authentication);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void canReadShouldReturnFalseWhenUserHasNoReadRole() {
        // Given
        mockAuthorities(Collections.singletonList(new SimpleGrantedAuthority(USER_ROLE)));

        // When
        boolean result = authorizationManager.canRead(authentication);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void canReadShouldReturnFalseWhenUserHasNoAuthorities() {
        // Given
        mockAuthorities(Collections.emptyList());

        // When
        boolean result = authorizationManager.canRead(authentication);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void canWriteShouldReturnTrueWhenUserHasAdminRole() {
        // Given
        mockAuthorities(Collections.singletonList(new SimpleGrantedAuthority(ADMIN_ROLE)));

        // When
        boolean result = authorizationManager.canWrite(authentication);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void canWriteShouldReturnFalseWhenUserHasOnlyReadRole() {
        // Given
        mockAuthorities(Collections.singletonList(new SimpleGrantedAuthority(READ_ROLE)));

        // When
        boolean result = authorizationManager.canWrite(authentication);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void canWriteShouldReturnFalseWhenUserHasNoAuthorities() {
        // Given
        mockAuthorities(Collections.emptyList());

        // When
        boolean result = authorizationManager.canWrite(authentication);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void canWriteShouldReturnTrueWhenUserHasMultipleRolesIncludingAdminRole() {
        // Given
        mockAuthorities(Arrays.asList(
                new SimpleGrantedAuthority(USER_ROLE),
                new SimpleGrantedAuthority(READ_ROLE),
                new SimpleGrantedAuthority(ADMIN_ROLE)
        ));

        // When
        boolean result = authorizationManager.canWrite(authentication);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void canDeleteShouldReturnTrueWhenUserHasAdminRole() {
        // Given
        mockAuthorities(Collections.singletonList(new SimpleGrantedAuthority(ADMIN_ROLE)));

        // When
        boolean result = authorizationManager.canDelete(authentication);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void canDeleteShouldReturnFalseWhenUserHasOnlyReadRole() {
        // Given
        mockAuthorities(Collections.singletonList(new SimpleGrantedAuthority(READ_ROLE)));

        // When
        boolean result = authorizationManager.canDelete(authentication);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void canDeleteShouldReturnFalseWhenUserHasNoAuthorities() {
        // Given
        mockAuthorities(Collections.emptyList());

        // When
        boolean result = authorizationManager.canDelete(authentication);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void canDeleteShouldReturnTrueWhenUserHasMultipleRolesIncludingAdminRole() {
        // Given
        mockAuthorities(Arrays.asList(
                new SimpleGrantedAuthority(USER_ROLE),
                new SimpleGrantedAuthority(ADMIN_ROLE)
        ));

        // When
        boolean result = authorizationManager.canDelete(authentication);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void allPermissionsShouldWorkCorrectlyWhenUserHasAllRoles() {
        // Given
        mockAuthorities(Arrays.asList(
                new SimpleGrantedAuthority(READ_ROLE),
                new SimpleGrantedAuthority(ADMIN_ROLE)
        ));

        // When & Then
        assertThat(authorizationManager.canRead(authentication)).isTrue();
        assertThat(authorizationManager.canWrite(authentication)).isTrue();
        assertThat(authorizationManager.canDelete(authentication)).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void canReadShouldReturnFalseWhenReadRolesArrayIsEmpty() {
        // Given
        LnsAuthorizationManager emptyRolesManager = new LnsAuthorizationManager() {
            @Override
            protected String[] getReadRoles() {
                return new String[0];
            }

            @Override
            protected String[] getAdminRoles() {
                return new String[] { ADMIN_ROLE };
            }
        };
        Collection<? extends GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority(READ_ROLE)
        );
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);

        // When
        boolean result = emptyRolesManager.canRead(authentication);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void canWriteShouldReturnFalseWhenAdminRolesArrayIsEmpty() {
        // Given
        LnsAuthorizationManager emptyAdminRolesManager = new LnsAuthorizationManager() {
            @Override
            protected String[] getReadRoles() {
                return new String[] { READ_ROLE };
            }

            @Override
            protected String[] getAdminRoles() {
                return new String[0];
            }
        };
        Collection<? extends GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority(ADMIN_ROLE)
        );
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);

        // When
        boolean result = emptyAdminRolesManager.canWrite(authentication);

        // Then
        assertThat(result).isFalse();
    }

    /**
     * Concrete implementation of LnsAuthorizationManager for testing purposes
     */
    private static class TestLnsAuthorizationManager extends LnsAuthorizationManager {
        @Override
        protected String[] getReadRoles() {
            return new String[] { READ_ROLE, ADMIN_ROLE };
        }

        @Override
        protected String[] getAdminRoles() {
            return new String[] { ADMIN_ROLE };
        }
    }
}

