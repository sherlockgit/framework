package com.sherlock.framework.tenant;

import com.sherlock.framework.base.error.BusinessException;
import com.sherlock.framework.base.error.ErrorCode;

import java.util.Objects;
import java.util.Optional;

public final class TenantContext {

    private static final ErrorCode MISSING_TENANT = new ErrorCode(
            "TENANT-400-001", "Tenant context is required");
    private static final ThreadLocal<TenantId> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static Optional<TenantId> current() {
        return Optional.ofNullable(CURRENT_TENANT.get());
    }

    public static TenantId require() {
        return current().orElseThrow(() -> new BusinessException(MISSING_TENANT));
    }

    public static void set(TenantId tenantId) {
        CURRENT_TENANT.set(Objects.requireNonNull(tenantId, "tenantId must not be null"));
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }

    public static void executeWith(TenantId tenantId, Runnable action) {
        Objects.requireNonNull(action, "action must not be null");
        TenantContextSnapshot previous = TenantContextSnapshot.capture();
        set(tenantId);
        try {
            action.run();
        } finally {
            previous.restore();
        }
    }
}
