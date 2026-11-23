package com.sherlock.framework.security.web;

import com.sherlock.framework.security.AuthenticatedPrincipal;
import com.sherlock.framework.security.PrincipalContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.core.Ordered;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class PrincipalContextFilter extends OncePerRequestFilter implements Ordered {

    private final StrictJwtValidator jwtValidator;

    public PrincipalContextFilter() {
        this(null);
    }

    public PrincipalContextFilter(StrictJwtValidator jwtValidator) {
        this.jwtValidator = jwtValidator;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        PrincipalContext.clear();
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && authentication.getPrincipal() instanceof AuthenticatedPrincipal principal) {
                PrincipalContext.set(principal);
            } else if (authentication != null && authentication.isAuthenticated()
                    && authentication.getPrincipal() instanceof Jwt jwt) {
                try {
                    PrincipalContext.set(principalFrom(jwt));
                } catch (RuntimeException exception) {
                    SecurityContextHolder.clearContext();
                    response.setHeader("WWW-Authenticate", "Bearer error=\"invalid_token\"");
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            PrincipalContext.clear();
        }
    }

    @Override
    public int getOrder() {
        return SecurityProperties.DEFAULT_FILTER_ORDER + 1;
    }

    private AuthenticatedPrincipal principalFrom(Jwt jwt) {
        if (jwtValidator != null) {
            return jwtValidator.validateAndCreatePrincipal(jwt);
        }
        throw new OAuth2AuthenticationException(new OAuth2Error("invalid_token"), "JWT validation is not configured");
    }
}
