package com.sherlock.framework.tenant;

import java.util.Objects;
import java.util.Optional;

public record TenantContextSnapshot(Optional<TenantId> tenantId) {

    public TenantContextSnapshot {
        tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
    }

    public static TenantContextSnapshot capture() {
        return new TenantContextSnapshot(TenantContext.current());
    }

    public void restore() {
        if (tenantId.isPresent()) {
            TenantContext.set(tenantId.get());
        } else {
            TenantContext.clear();
        }
    }
}
