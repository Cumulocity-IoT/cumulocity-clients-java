package com.cumulocity.lpwan.exception;

import com.cumulocity.lpwan.lns.connection.model.LnsConnectionDeserializer;

/**
 * The user who set up the integration was refused a user management call the first-time setup needs.
 * <p>
 * The agent changes users with the rights of the calling user, not its own service user, so the account a
 * network server authenticates with can only be created by a user with user management admin rights. Once
 * it exists, users without those rights can register devices.
 * <p>
 * Unchecked so that it can be thrown from methods whose signatures agents already compile against. Answered
 * with 403 by {@link com.cumulocity.lpwan.handler.LpwanExceptionsHandler}.
 */
public class LpwanUserManagementNotPermittedException extends RuntimeException {

    public LpwanUserManagementNotPermittedException(Throwable cause) {
        super(String.format("Setting up the %s integration for the first time requires a user with user "
                + "management admin rights.", LnsConnectionDeserializer.getRegisteredAgentName()), cause);
    }
}
