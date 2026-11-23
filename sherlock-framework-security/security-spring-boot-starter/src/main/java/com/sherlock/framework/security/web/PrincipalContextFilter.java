package com.sherlock.framework.security.web;

import com.sherlock.framework.security.AuthenticatedPrincipal;
import com.sherlock.framework.security.PrincipalContext;
import com.sherlock.framework.tenant.TenantId;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.core.Ordered;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class PrincipalContextFilter extends OncePerRequestFilter implements Ordered {

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
                PrincipalContext.set(principalFrom(jwt));
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

    private static AuthenticatedPrincipal principalFrom(Jwt jwt) {
        List<String> permissions = jwt.getClaimAsStringList("permissions");
        return new AuthenticatedPrincipal(
                Long.parseLong(jwt.getSubject()),
                new TenantId(longClaim(jwt, "tenant_id")),
                permissions == null ? Set.of() : Set.copyOf(permissions),
                longClaim(jwt, "policy_version"));
    }

    private static long longClaim(Jwt jwt, String claimName) {
        Object value = jwt.getClaim(claimName);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            return Long.parseLong(text);
        }
        throw new IllegalArgumentException(claimName + " must be a number");
    }
}
