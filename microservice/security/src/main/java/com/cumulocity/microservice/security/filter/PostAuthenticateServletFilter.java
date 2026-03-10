package com.cumulocity.microservice.security.filter;

import com.cumulocity.microservice.context.ContextService;
import com.cumulocity.microservice.context.credentials.Credentials;
import com.cumulocity.microservice.security.filter.provider.PostAuthorizationContextProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

import static com.google.common.collect.FluentIterable.from;

@Slf4j
public class PostAuthenticateServletFilter extends OncePerRequestFilter {

    private final List<PostAuthorizationContextProvider<SecurityContext>> credentialsResolvers;
    private final ContextService<Credentials> contextService;

    public PostAuthenticateServletFilter(List<PostAuthorizationContextProvider<SecurityContext>> credentialsResolvers,
                                         ContextService<Credentials> contextService) {
        this.credentialsResolvers = credentialsResolvers;
        this.contextService = contextService;
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain) throws IOException, ServletException {
        Runnable runnable = () -> {
            try {
                filterChain.doFilter(request, response);
            } catch (final Exception ex) {
                Throwables.propagate(ex);
            }
        };
        if (contextService == null) {
            log.warn("Context service not available.");
        }

        if (contextService != null && credentialsResolvers != null) {
            try {
                final ImmutableList<Credentials> credentials = from(credentialsResolvers)
                        .filter(provider -> provider.supports(SecurityContextHolder.getContext()))
                        .transform(provider -> provider.get(SecurityContextHolder.getContext()))
                        .filter(credentials1 -> credentials1 != null).toList();

                for (final Credentials credential : credentials) {
                    runnable = contextService.withinContext(credential, runnable);
                }
            } catch (AccessDeniedException e) {
                sendAccessDenied(response, e.getMessage());
                return;
            }
        }

        runnable.run();
    }

    private void sendAccessDenied(HttpServletResponse response, String errorMessage) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.getWriter().write(toJsonError("Microservice/Access Denied", errorMessage));
    }

    private String toJsonError(String error, String message) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("error", error);
        node.put("message", message);
        return node.toString();
    }
}
