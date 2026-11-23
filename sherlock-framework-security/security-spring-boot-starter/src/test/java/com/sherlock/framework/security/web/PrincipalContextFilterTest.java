package com.sherlock.framework.security.web;

import com.sherlock.framework.security.AuthenticatedPrincipal;
import com.sherlock.framework.security.PrincipalContext;
import com.sherlock.framework.tenant.TenantId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PrincipalContextFilterTest {

    private final PrincipalContextFilter filter = new PrincipalContextFilter();

    @AfterEach
    void clearContexts() {
        PrincipalContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void filterExposesAuthenticatedPrincipalOnlyDuringRequest() throws Exception {
        AuthenticatedPrincipal principal = new AuthenticatedPrincipal(7L, new TenantId(9L), Set.of("order:read"), 3L);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null,
                        AuthorityUtils.createAuthorityList("order:read")));

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) ->
                assertThat(PrincipalContext.require()).isEqualTo(principal));

        assertThat(PrincipalContext.current()).isEmpty();
    }

    @Test
    void filterClearsPrincipalWhenChainThrows() {
        AuthenticatedPrincipal principal = new AuthenticatedPrincipal(7L, new TenantId(9L), Set.of("order:read"), 3L);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null,
                        AuthorityUtils.createAuthorityList("order:read")));

        assertThatThrownBy(() -> filter.doFilter(
                new MockHttpServletRequest(),
                new MockHttpServletResponse(),
                (request, response) -> {
                    assertThat(PrincipalContext.require()).isEqualTo(principal);
                    throw new IllegalStateException("boom");
                }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");

        assertThat(PrincipalContext.current()).isEmpty();
    }

    @Test
    void filterRunsAfterSpringSecurity() {
        assertThat(filter.getOrder()).isEqualTo(SecurityProperties.DEFAULT_FILTER_ORDER + 1);
    }

    @Test
    void filterDoesNotExposeStalePrincipalForUnauthenticatedRequest() throws Exception {
        PrincipalContext.set(new AuthenticatedPrincipal(99L, new TenantId(9L), Set.of("admin"), 1L));

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) ->
                assertThat(PrincipalContext.current()).isEmpty());
    }
}
