package com.sherlock.framework.tenant;

public record TenantId(long value) {

    public TenantId {
        if (value <= 0) {
            throw new IllegalArgumentException("Tenant ID must be positive");
        }
    }
}
