package com.cumulocity.microservice.security.filter;

import com.cumulocity.microservice.context.ContextService;
import com.cumulocity.microservice.context.credentials.Credentials;
import com.cumulocity.microservice.security.filter.provider.PreAuthorizationContextProvider;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static com.google.common.collect.FluentIterable.from;

@Slf4j
public class PreAuthenticateServletFilter extends OncePerRequestFilter {

    private final List<PreAuthorizationContextProvider<HttpServletRequest>> credentialsResolvers;
    private final ContextService<Credentials> contextService;

    public PreAuthenticateServletFilter(List<PreAuthorizationContextProvider<HttpServletRequest>> credentialsResolvers,
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
        if (contextService != null
                && credentialsResolvers != null
                && !contextService.isInContext()
        ) {
            final ImmutableList<Credentials> credentials = from(credentialsResolvers)
                    .filter(provider -> provider.supports(request))
                    .transform(provider -> provider.get(request))
                    .filter(Objects::nonNull)
                    .toList();

            for (final Credentials credential : credentials) {
                runnable = contextService.withinContext(credential, runnable);
            }
        }

        runnable.run();
    }

}
