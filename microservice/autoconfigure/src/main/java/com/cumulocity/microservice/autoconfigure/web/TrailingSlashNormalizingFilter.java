package com.cumulocity.microservice.autoconfigure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Restores the pre-Spring-Boot-3.0 trailing-slash URL behaviour for microservices.
 */
public class TrailingSlashNormalizingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        final String uri = request.getRequestURI();
        if (uri != null && uri.length() > 1 && uri.endsWith("/")
                && !uri.equals(request.getContextPath() + "/")) {
            filterChain.doFilter(new TrailingSlashStrippedRequest(request), response);
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private static final class TrailingSlashStrippedRequest extends HttpServletRequestWrapper {
        TrailingSlashStrippedRequest(HttpServletRequest request) {
            super(request);
        }

        private static String stripTrailingSlash(String value) {
            return (value != null && value.length() > 1 && value.endsWith("/"))
                    ? value.substring(0, value.length() - 1)
                    : value;
        }

        @Override
        public String getRequestURI() {
            return stripTrailingSlash(super.getRequestURI());
        }

        @Override
        public String getServletPath() {
            return stripTrailingSlash(super.getServletPath());
        }

        @Override
        public String getPathInfo() {
            return stripTrailingSlash(super.getPathInfo());
        }

        @Override
        public StringBuffer getRequestURL() {
            StringBuffer url = super.getRequestURL();
            if (url.length() > 1 && url.charAt(url.length() - 1) == '/') {
                url.setLength(url.length() - 1);
            }
            return url;
        }
    }
}
