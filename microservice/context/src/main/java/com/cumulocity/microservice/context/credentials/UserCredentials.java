package com.cumulocity.microservice.context.credentials;

public class UserCredentials implements Credentials {
    private String tenant;
    private String username;
    private String password;
    private String oAuthAccessToken;
    private String xsrfToken;
    private String identifier;
    private String tfaToken;
    private String appKey;
    private String origin;

    @java.beans.ConstructorProperties({"tenant", "username", "password", "oAuthAccessToken", "xsrfToken","identifier", "tfaToken", "appKey"})
    public UserCredentials(String tenant, String username, String password, String oAuthAccessToken, String xsrfToken, String identifier, String tfaToken, String appKey) {
        this.tenant = tenant;
        this.username = username;
        this.password = password;
        this.oAuthAccessToken = oAuthAccessToken;
        this.xsrfToken = xsrfToken;
        this.identifier = identifier;
        this.tfaToken = tfaToken;
        this.appKey = appKey;
    }

    @java.beans.ConstructorProperties({"tenant", "username", "password", "oAuthAccessToken", "xsrfToken","identifier", "tfaToken", "appKey", "origin"})
    public UserCredentials(String tenant, String username, String password, String oAuthAccessToken, String xsrfToken, String identifier, String tfaToken, String appKey, String origin) {
        this.tenant = tenant;
        this.username = username;
        this.password = password;
        this.oAuthAccessToken = oAuthAccessToken;
        this.xsrfToken = xsrfToken;
        this.identifier = identifier;
        this.tfaToken = tfaToken;
        this.appKey = appKey;
        this.origin = origin;
    }

    public UserCredentials() {
    }

    public static UserCredentialsBuilder builder() {
        return new UserCredentialsBuilder();
    }

    public String getTenant() {
        return this.tenant;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public String getOAuthAccessToken() {
        return this.oAuthAccessToken;
    }

    public String getXsrfToken() {
        return this.xsrfToken;
    }

    public String getIdentifier() {
        return this.identifier;
    }

    public String getTfaToken() {
        return this.tfaToken;
    }

    public String getAppKey() {
        return this.appKey;
    }

    public String getOrigin() {
        return this.origin;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setOAuthAccessToken(String oAuthAccessToken) {
        this.oAuthAccessToken = oAuthAccessToken;
    }

    public void setXsrfToken(String xsrfToken) {
        this.xsrfToken = xsrfToken;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public void setTfaToken(String tfaToken) {
        this.tfaToken = tfaToken;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof UserCredentials)) return false;
        final UserCredentials other = (UserCredentials) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$tenant = this.getTenant();
        final Object other$tenant = other.getTenant();
        if (this$tenant == null ? other$tenant != null : !this$tenant.equals(other$tenant)) return false;
        final Object this$username = this.getUsername();
        final Object other$username = other.getUsername();
        if (this$username == null ? other$username != null : !this$username.equals(other$username)) return false;
        final Object this$password = this.getPassword();
        final Object other$password = other.getPassword();
        if (this$password == null ? other$password != null : !this$password.equals(other$password)) return false;
        final Object this$oAuthAccessToken = this.getOAuthAccessToken();
        final Object other$oAuthAccessToken = other.getOAuthAccessToken();
        if (this$oAuthAccessToken == null ? other$oAuthAccessToken != null : !this$oAuthAccessToken.equals(oAuthAccessToken)) return false;
        final Object this$xsrfToken = this.getXsrfToken();
        final Object other$xsrfToken = other.getXsrfToken();
        if (this$xsrfToken == null ? other$xsrfToken != null : !this$xsrfToken.equals(xsrfToken)) return false;
        final Object this$identifier = this.getIdentifier();
        final Object other$identifier = other.getIdentifier();
        if (this$identifier == null ? other$identifier != null : !this$identifier.equals(other$identifier))
            return false;
        final Object this$tfaToken = this.getTfaToken();
        final Object other$tfaToken = other.getTfaToken();
        if (this$tfaToken == null ? other$tfaToken != null : !this$tfaToken.equals(other$tfaToken)) return false;
        final Object this$appKey = this.getAppKey();
        final Object other$appKey = other.getAppKey();
        if (this$appKey == null ? other$appKey != null : !this$appKey.equals(other$appKey)) return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $tenant = this.getTenant();
        result = result * PRIME + ($tenant == null ? 43 : $tenant.hashCode());
        final Object $username = this.getUsername();
        result = result * PRIME + ($username == null ? 43 : $username.hashCode());
        final Object $password = this.getPassword();
        result = result * PRIME + ($password == null ? 43 : $password.hashCode());
        final Object $oAuthAccessToken = this.getOAuthAccessToken();
        result = result * PRIME + ($oAuthAccessToken == null ? 43 : oAuthAccessToken.hashCode());
        final Object $xsrfToken = this.getXsrfToken();
        result = result * PRIME + ($xsrfToken == null ? 43 : xsrfToken.hashCode());
        final Object $identifier = this.getIdentifier();
        result = result * PRIME + ($identifier == null ? 43 : $identifier.hashCode());
        final Object $tfaToken = this.getTfaToken();
        result = result * PRIME + ($tfaToken == null ? 43 : $tfaToken.hashCode());
        final Object $appKey = this.getAppKey();
        result = result * PRIME + ($appKey == null ? 43 : $appKey.hashCode());
        return result;
    }

    protected boolean canEqual(Object other) {
        return other instanceof UserCredentials;
    }

    public UserCredentials withTenant(String tenant) {
        return this.tenant == tenant ? this : new UserCredentials(tenant, this.username, this.password, this.oAuthAccessToken, this.xsrfToken, this.identifier, this.tfaToken, this.appKey, this.origin);
    }

    public UserCredentials withUsername(String username) {
        return this.username == username ? this : new UserCredentials(this.tenant, username, this.password, this.oAuthAccessToken, this.xsrfToken, this.identifier, this.tfaToken, this.appKey, this.origin);
    }

    public UserCredentials withPassword(String password) {
        return this.password == password ? this : new UserCredentials(this.tenant, this.username, password, this.oAuthAccessToken, this.xsrfToken, this.identifier, this.tfaToken, this.appKey, this.origin);
    }

    public UserCredentials withIdentifier(String identifier) {
        return this.identifier == identifier ? this : new UserCredentials(this.tenant, this.username, this.password, this.oAuthAccessToken, this.xsrfToken, identifier, this.tfaToken, this.appKey, this.origin);
    }

    public UserCredentials withOAuthAccessToken(String oAuthAccessToken) {
        return this.oAuthAccessToken == oAuthAccessToken ? this : new UserCredentials(this.tenant, this.username, this.password, oAuthAccessToken, this.xsrfToken, this.identifier, this.tfaToken, this.appKey, this.origin);
    }

    public UserCredentials withXsrfToken(String xsrfToken) {
        return this.xsrfToken == xsrfToken ? this : new UserCredentials(this.tenant, this.username, this.password, this.oAuthAccessToken, xsrfToken, this.identifier, this.tfaToken, this.appKey, this.origin);
    }

    public UserCredentials withTfaToken(String tfaToken) {
        return this.tfaToken == tfaToken ? this : new UserCredentials(this.tenant, this.username, this.password, this.oAuthAccessToken, this.xsrfToken, this.identifier, tfaToken, this.appKey, this.origin);
    }

    public UserCredentials withAppKey(String appKey) {
        return this.appKey == appKey ? this : new UserCredentials(this.tenant, this.username, this.password, this.oAuthAccessToken, this.xsrfToken, this.identifier, this.tfaToken, appKey, this.origin);
    }

    public String toString() {
        return "UserCredentials(tenant=" + this.getTenant() + ", username=" + this.getUsername() + ")";
    }

    public static class UserCredentialsBuilder {
        private String tenant;
        private String username;
        private String password;
        private String oAuthAccessToken;
        private String xsrfToken;
        private String identifier;
        private String tfaToken;
        private String appKey;
        private String origin;

        UserCredentialsBuilder() {
        }

        public UserCredentials.UserCredentialsBuilder tenant(String tenant) {
            this.tenant = tenant;
            return this;
        }

        public UserCredentials.UserCredentialsBuilder username(String username) {
            this.username = username;
            return this;
        }

        public UserCredentials.UserCredentialsBuilder oAuthAccessToken(String oAuthAccessToken) {
            this.oAuthAccessToken = oAuthAccessToken;
            return this;
        }

        public UserCredentials.UserCredentialsBuilder xsrfToken(String xsrfToken) {
            this.xsrfToken = xsrfToken;
            return this;
        }

        public UserCredentials.UserCredentialsBuilder password(String password) {
            this.password = password;
            return this;
        }

        public UserCredentials.UserCredentialsBuilder identifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public UserCredentials.UserCredentialsBuilder tfaToken(String tfaToken) {
            this.tfaToken = tfaToken;
            return this;
        }

        public UserCredentials.UserCredentialsBuilder appKey(String appKey) {
            this.appKey = appKey;
            return this;
        }

        public UserCredentials.UserCredentialsBuilder origin(String origin) {
            this.origin = origin;
            return this;
        }

        public UserCredentials build() {
            return new UserCredentials(tenant, username, password, oAuthAccessToken, xsrfToken, identifier, tfaToken, appKey, origin);
        }

        public String toString() {
            return "UserCredentials.UserCredentialsBuilder(tenant=" + this.tenant + ", username=" + this.username + ", password=" + this.password + ", oAuthAccessToken=" + this.oAuthAccessToken + ", xsrfToken=" + this.xsrfToken + ", identifier=" + this.identifier + ", tfaToken=" + this.tfaToken + ", appKey=" + this.appKey + ", origin=" + this.origin +")";
        }
    }
}
