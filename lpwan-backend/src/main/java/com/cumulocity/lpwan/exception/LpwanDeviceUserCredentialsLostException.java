package com.cumulocity.lpwan.exception;

/**
 * The account a network server authenticates with exists, but the agent no longer holds its password.
 * <p>
 * The platform issues a device user's password once and cannot be asked for it again, so this cannot be
 * recovered. It is deliberately not resolved by provisioning the account again: the platform would reset
 * the password of the existing account, and every network server still configured with the old one would
 * silently stop delivering.
 * <p>
 * Extends {@link LpwanServiceException} so that it is answered with 500. This is a server side condition, the
 * caller cannot do anything about. Recovering it means restoring the stored credentials, or deleting the
 * account so that it can be provisioned from scratch and every output re-pushed.
 */
public class LpwanDeviceUserCredentialsLostException extends LpwanServiceException {

    public LpwanDeviceUserCredentialsLostException(String message) {
        super(message);
    }

    public LpwanDeviceUserCredentialsLostException(String message, Throwable cause) {
        super(message, cause);
    }
}
