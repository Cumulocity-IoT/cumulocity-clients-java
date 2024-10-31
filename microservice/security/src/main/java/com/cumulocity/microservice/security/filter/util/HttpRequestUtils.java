package com.cumulocity.microservice.security.filter.util;

import org.bouncycastle.util.encoders.Base64;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

public class HttpRequestUtils {
    public static final String X_CUMULOCITY_APPLICATION_KEY = "X-Cumulocity-Application-Key";
    public static final String TFA_TOKEN_HEADER = "TFAToken";
    public static final String LOGIN_SEPARATOR = "/";
    public static final String AUTH_PREFIX = "BASIC ";
    public static final String AUTH_SEPARATOR = ":";
    public static final String XSRF_TOKEN_HEADER = "X-XSRF-TOKEN";

    public static class AuthorizationHeader {
        private String tenant;
        private String username;
        private String password;

        @java.beans.ConstructorProperties({"tenant", "username", "password"})
        AuthorizationHeader(String tenant, String username, String password) {
            this.tenant = tenant;
            this.username = username;
            this.password = password;
        }

        public static AuthorizationHeaderBuilder builder() {
            return new AuthorizationHeaderBuilder();
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

        public void setTenant(String tenant) {
            this.tenant = tenant;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof AuthorizationHeader)) return false;
            final AuthorizationHeader other = (AuthorizationHeader) o;
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
            return result;
        }

        protected boolean canEqual(Object other) {
            return other instanceof AuthorizationHeader;
        }

        public String toString() {
            return "HttpRequestUtils.AuthorizationHeader(tenant=" + this.getTenant() + ", username=" + this.getUsername() + ", password=" + this.getPassword() + ")";
        }

        public static class AuthorizationHeaderBuilder {
            private String tenant;
            private String username;
            private String password;

            AuthorizationHeaderBuilder() {
            }

            public AuthorizationHeader.AuthorizationHeaderBuilder tenant(String tenant) {
                this.tenant = tenant;
                return this;
            }

            public AuthorizationHeader.AuthorizationHeaderBuilder username(String username) {
                this.username = username;
                return this;
            }

            public AuthorizationHeader.AuthorizationHeaderBuilder password(String password) {
                this.password = password;
                return this;
            }

            public AuthorizationHeader build() {
                return new AuthorizationHeader(tenant, username, password);
            }

            public String toString() {
                return "HttpRequestUtils.AuthorizationHeader.AuthorizationHeaderBuilder(tenant=" + this.tenant + ", username=" + this.username + ", password=" + this.password + ")";
            }
        }
    }

    public static boolean hasAuthorizationHeader(Object credentialSource) {
        return credentialSource instanceof HttpServletRequest
                && StringUtils.hasText(((HttpServletRequest) credentialSource).getHeader(AUTHORIZATION));
    }

    public static AuthorizationHeader authorizationHeader(String authorization) {
        String tenant = null;
        String username = null;
        String password = null;

        if (authorization != null) {
            String[] loginAndPass = decode(authorization);

            tenant = null;
            username = loginAndPass.length > 0 ? loginAndPass[0] : "";
            password = loginAndPass.length > 1 ? loginAndPass[1] : "";
            final String[] tenantAndUsername = splitUsername(username);
            if (tenantAndUsername.length > 1) {
                tenant = tenantAndUsername[0];
                username = tenantAndUsername[1];
            }
        }

        return AuthorizationHeader.builder()
                .tenant(tenant)
                .username(username)
                .password(password)
                .build();
    }

    public static String[] splitUsername(String username) {
        return username.split(LOGIN_SEPARATOR);
    }

    public static String[] decode(String authorization) {
        final byte[] decoded = Base64.decode(authorization.substring(AUTH_PREFIX.length()).getBytes());
        return new String(decoded, StandardCharsets.UTF_8)
                .split(AUTH_SEPARATOR, 2);
    }
}
