package com.sherlock.framework.security;

import com.sherlock.framework.base.error.BusinessException;
import com.sherlock.framework.tenant.TenantId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PrincipalContextTest {

    @AfterEach
    void clearContext() {
        PrincipalContext.clear();
    }

    @Test
    void principalCapturesAnImmutablePermissionSet() {
        Set<String> permissions = new HashSet<>(Set.of("order:read"));

        AuthenticatedPrincipal principal = new AuthenticatedPrincipal(7L, new TenantId(9L), permissions, 3L);
        permissions.add("order:write");

        assertThat(principal.permissions()).containsExactly("order:read");
        assertThatThrownBy(() -> principal.permissions().add("admin"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void requireWithoutPrincipalUsesStableSecurityErrorCode() {
        assertThatThrownBy(PrincipalContext::require)
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode().value())
                .isEqualTo("SECURITY-401-001");
    }

    @Test
    void clearRemovesPrincipalFromCurrentThread() {
        PrincipalContext.set(new AuthenticatedPrincipal(7L, new TenantId(9L), Set.of("order:read"), 3L));

        PrincipalContext.clear();

        assertThat(PrincipalContext.current()).isEmpty();
    }
}
