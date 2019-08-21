package com.cumulocity.microservice.security.token;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import lombok.experimental.UtilityClass;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

@UtilityClass
public final class CookieReader {

    public static final String AUTHORIZATION_KEY = "authorization";

    public static Optional<Cookie> readAuthorizationCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.absent();
        }
        return FluentIterable.from(Lists.newArrayList(cookies)).firstMatch(new Predicate<Cookie>() {
            @Override
            public boolean apply(Cookie cookie) {
                return AUTHORIZATION_KEY.equals(cookie.getName());
            }
        });
    }

}
