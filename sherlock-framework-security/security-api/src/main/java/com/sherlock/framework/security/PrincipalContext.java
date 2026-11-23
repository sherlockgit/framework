package com.sherlock.framework.security;

import com.sherlock.framework.base.error.BusinessException;
import com.sherlock.framework.base.error.ErrorCode;

import java.util.Objects;
import java.util.Optional;

public final class PrincipalContext {

    private static final ErrorCode MISSING_PRINCIPAL = new ErrorCode(
            "SECURITY-401-001", "Authenticated principal is required");
    private static final ThreadLocal<AuthenticatedPrincipal> CURRENT_PRINCIPAL = new ThreadLocal<>();

    private PrincipalContext() {
    }

    public static Optional<AuthenticatedPrincipal> current() {
        return Optional.ofNullable(CURRENT_PRINCIPAL.get());
    }

    public static AuthenticatedPrincipal require() {
        return current().orElseThrow(() -> new BusinessException(MISSING_PRINCIPAL));
    }

    public static void set(AuthenticatedPrincipal principal) {
        CURRENT_PRINCIPAL.set(Objects.requireNonNull(principal, "principal must not be null"));
    }

    public static void clear() {
        CURRENT_PRINCIPAL.remove();
    }
}
