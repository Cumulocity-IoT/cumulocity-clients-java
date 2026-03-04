package com.cumulocity.lpwan.lns.connection.manager;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Arrays;
import java.util.Collection;

/**
 * Abstract authorization manager for LNS connection operations.
 * <p>
 * This class provides role-based access control for read, write, and delete operations
 * on LNS connections. Subclasses must define the specific roles required for read and
 * administrative operations.
 * </p>
 *
 */
public abstract class LnsAuthorizationManager {

    /**
     * Returns the roles that are allowed to perform read operations.
     * <p>
     * This method should return an array of role names that have read permissions
     * for LNS connection resources.
     * </p>
     *
     * @return array of role names with read permissions
     */
    protected abstract String[] getReadRoles();

    /**
     * Returns the roles that are allowed to perform administrative operations.
     * <p>
     * This method should return an array of role names that have administrative
     * permissions for LNS connection resources, including write and delete operations.
     * </p>
     *
     * @return array of role names with administrative permissions
     */
    protected abstract String[] getAdminRoles();

    /**
     * Checks if the authenticated user has permission to read LNS connection resources.
     * <p>
     * A user is authorized to read if they have any of the roles defined by {@link #getReadRoles()}.
     * </p>
     *
     * @param authentication the authentication object containing user credentials and authorities
     * @return {@code true} if the user has read permission, {@code false} otherwise
     */
    public final boolean canRead(Authentication authentication) {
        return hasAnyRole(authentication, getReadRoles());
    }

    /**
     * Checks if the authenticated user has permission to write/modify LNS connection resources.
     * <p>
     * A user is authorized to write if they have any of the roles defined by {@link #getAdminRoles()}.
     * </p>
     *
     * @param authentication the authentication object containing user credentials and authorities
     * @return {@code true} if the user has write permission, {@code false} otherwise
     */
    public final boolean canWrite(Authentication authentication) {
        return hasAnyRole(authentication, getAdminRoles());
    }

    /**
     * Checks if the authenticated user has permission to delete LNS connection resources.
     * <p>
     * A user is authorized to delete if they have any of the roles defined by {@link #getAdminRoles()}.
     * </p>
     *
     * @param authentication the authentication object containing user credentials and authorities
     * @return {@code true} if the user has delete permission, {@code false} otherwise
     */
    public final boolean canDelete(Authentication authentication) {
        return hasAnyRole(authentication, getAdminRoles());
    }

    /**
     * Checks if the authenticated user has any of the specified roles.
     * <p>
     * This method verifies that at least one of the provided roles matches
     * one of the user's granted authorities.
     * </p>
     *
     * @param auth the authentication object containing user credentials and authorities
     * @param roles the roles to check against
     * @return {@code true} if the user has at least one of the specified roles, {@code false} otherwise
     */
    private boolean hasAnyRole(Authentication auth, String... roles) {
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        return Arrays.stream(roles)
                .anyMatch(role -> authorities.stream()
                        .anyMatch(a -> a.getAuthority().equals(role)));
    }

    /**
     * Checks if the authenticated user has all of the specified roles.
     * <p>
     * This method verifies that all of the provided roles match
     * the user's granted authorities.
     * </p>
     *
     * @param auth the authentication object containing user credentials and authorities
     * @param roles the roles to check against
     * @return {@code true} if the user has all of the specified roles, {@code false} otherwise
     */
    private boolean hasAllRoles(Authentication auth, String... roles) {
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        return Arrays.stream(roles)
                .allMatch(role -> authorities.stream()
                        .anyMatch(a -> a.getAuthority().equals(role)));
    }
}
