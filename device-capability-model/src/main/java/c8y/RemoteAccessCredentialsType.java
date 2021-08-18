package c8y;

import org.springframework.util.StringUtils;

public enum RemoteAccessCredentialsType {

    NONE(new CredentialsValidator() {
        @Override
        public void validateCredentials(RemoteAccessCredentials credentials) {
            return;
        }
    }),
    PASS_ONLY(new CredentialsValidator() {
        @Override
        public void validateCredentials(RemoteAccessCredentials credentials) {
            validateValueNotEmpty(credentials.getPassword(), "Password", PASS_ONLY);
        }
    }),
    USER_PASS(new CredentialsValidator() {
        @Override
        public void validateCredentials(RemoteAccessCredentials credentials) {
            validateValueNotEmpty(credentials.getUsername(), "User", USER_PASS);
            validateValueNotEmpty(credentials.getPassword(), "Password", USER_PASS);
        }
    }),
    KEY_PAIR(new CredentialsValidator() {
        @Override
        public void validateCredentials(RemoteAccessCredentials credentials) {
            validateValueNotEmpty(credentials.getUsername(), "User", KEY_PAIR);
            validateValueNotEmpty(credentials.getPrivateKey(), "Private key", KEY_PAIR);
        }
    }),
    CERTIFICATE(new CredentialsValidator() {
        @Override
        public void validateCredentials(RemoteAccessCredentials credentials) {
            validateValueNotEmpty(credentials.getUsername(), "User", CERTIFICATE);
            validateValueNotEmpty(credentials.getPrivateKey(), "Private key", CERTIFICATE);
            validateValueNotEmpty(credentials.getCertificate(), "Certificate", CERTIFICATE);
        }
    });

    public static RemoteAccessCredentialsType fromString(String type) {
        if (StringUtils.isEmpty(type)) {
            return null;
        }
        return valueOf(type);
    }

    private static void validateValueNotEmpty(String valueToCheck, final String fieldName, RemoteAccessCredentialsType credentialsType) {
        if (StringUtils.isEmpty(valueToCheck)) {
            throw new RemoteAccessConfigurationException("%s must be provided in %s credentials", fieldName, credentialsType);
        }
    }

    private final CredentialsValidator credentialsValidator;

    RemoteAccessCredentialsType(CredentialsValidator credentialsValidator) {
        this.credentialsValidator = credentialsValidator;
    }

    public void validateCredentials(RemoteAccessCredentials credentials) {
        this.credentialsValidator.validateCredentials(credentials);
    }

    private interface CredentialsValidator {
        void validateCredentials(RemoteAccessCredentials credentials);
    }
}
