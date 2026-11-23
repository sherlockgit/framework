package com.sherlock.framework.security.jwt;

import com.sherlock.framework.tenant.TenantId;

import java.util.Objects;
import java.util.Set;

public record JwtClaims(long userId, TenantId tenantId, Set<String> permissions, long policyVersion, String tokenId) {

    public JwtClaims {
        if (userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
        tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
        permissions = immutablePermissions(permissions);
        if (policyVersion < 0) {
            throw new IllegalArgumentException("policyVersion must not be negative");
        }
        if (tokenId == null || tokenId.isBlank()) {
            throw new IllegalArgumentException("tokenId must not be blank");
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
