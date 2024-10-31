package com.cumulocity.microservice.security.filter;

import com.cumulocity.microservice.context.ContextService;
import com.cumulocity.microservice.context.credentials.Credentials;
import com.cumulocity.microservice.security.filter.provider.PreAuthorizationContextProvider;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

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
        Runnable runnable = new Runnable() {
            public void run() {
                try {
                    filterChain.doFilter(request, response);
                } catch (final Exception ex) {
                    Throwables.propagate(ex);
                }
            }
        };
        if (contextService != null
                && credentialsResolvers != null
                && !contextService.isInContext()
        ) {
            final ImmutableList<Credentials> credentials = from(credentialsResolvers)
                    .filter(new Predicate<PreAuthorizationContextProvider<HttpServletRequest>>() {
                        public boolean apply(PreAuthorizationContextProvider<HttpServletRequest> provider) {
                            return provider.supports(request);
                        }
                    })
                    .transform(new Function<PreAuthorizationContextProvider<HttpServletRequest>, Credentials>() {
                        public Credentials apply(PreAuthorizationContextProvider<HttpServletRequest> provider) {
                            return provider.get(request);
                        }
                    })
                    .filter(new Predicate<Credentials>() {
                        public boolean apply(Credentials credentials) {
                            return credentials != null;
                        }
                    })
                    .toList();

            for (final Credentials credential : credentials) {
                runnable = contextService.withinContext(credential, runnable);
            }
        }

        runnable.run();
    }

}
