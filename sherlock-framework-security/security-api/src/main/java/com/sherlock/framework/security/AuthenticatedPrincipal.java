package com.sherlock.framework.security;

import com.sherlock.framework.tenant.TenantId;

import java.util.Objects;
import java.util.Set;

public record AuthenticatedPrincipal(long userId, TenantId tenantId, Set<String> permissions, long policyVersion) {

    public AuthenticatedPrincipal {
        if (userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
        tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
        permissions = immutablePermissions(permissions);
        if (policyVersion < 0) {
            throw new IllegalArgumentException("policyVersion must not be negative");
        }
    }

    private static Set<String> immutablePermissions(Set<String> permissions) {
        Objects.requireNonNull(permissions, "permissions must not be null");
        if (permissions.stream().anyMatch(permission -> permission == null || permission.isBlank())) {
            throw new IllegalArgumentException("permissions must not contain blank values");
        }
        return Set.copyOf(permissions);
    }
}
