package com.sherlock.framework.data.scope;

import java.util.Objects;
import java.util.Optional;

public final class DataScopeContext {

    private static final ThreadLocal<DataScope> CURRENT_SCOPE = new ThreadLocal<>();

    private DataScopeContext() {
    }

    public static Optional<DataScope> current() {
        return Optional.ofNullable(CURRENT_SCOPE.get());
    }

    public static DataScope require() {
        return current().orElseThrow(() -> new IllegalStateException("Data scope context is required"));
    }

    public static void set(DataScope dataScope) {
        CURRENT_SCOPE.set(Objects.requireNonNull(dataScope, "dataScope must not be null"));
    }

    public static void clear() {
        CURRENT_SCOPE.remove();
    }
}
