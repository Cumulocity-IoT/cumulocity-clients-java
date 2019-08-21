package com.cumulocity.microservice.security.token;

import com.cumulocity.agent.server.context.DeviceCredentials;
import com.cumulocity.rest.representation.user.CurrentUserRepresentation;
import com.cumulocity.rest.representation.user.RoleRepresentation;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;

@RequiredArgsConstructor
public class JwtTokenAuthentication implements Authentication {

    private final JwtCredentials jwtCredentials;
    @Setter
    @Getter
    private boolean authenticated;
    @Setter
    @Getter
    private CurrentUserRepresentation currentUserRepresentation;
    @Setter
    @Getter
    private DeviceCredentials deviceCredentials;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // todo memoize it

        return Collections2.transform(currentUserRepresentation.getEffectiveRoles(), new Function<RoleRepresentation, GrantedAuthority>() {
            @Override
            public GrantedAuthority apply(RoleRepresentation roleRepresentation) {
                return new SimpleGrantedAuthority(roleRepresentation.getName());
            }
        });
    }

    @Override
    public Object getCredentials() {
        return jwtCredentials;
    }

    @Override
    public Object getDetails() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return currentUserRepresentation != null ? currentUserRepresentation.getUserName() : null;
    }

    @Override
    public String getName() {
        return currentUserRepresentation != null ? currentUserRepresentation.getUserName() : null;
    }
}
