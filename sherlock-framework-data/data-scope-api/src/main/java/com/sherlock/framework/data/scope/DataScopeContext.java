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

    /**
     * Captures the current scope for explicit propagation to an asynchronous or message-handling task.
     */
    public static Optional<DataScope> snapshot() {
        return current();
    }

    /**
     * Runs work with an explicitly captured scope and restores the worker's previous scope afterward.
     */
    public static void executeWith(Optional<DataScope> snapshot, Runnable task) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(task, "task must not be null");

        Optional<DataScope> previous = current();
        clear();
        snapshot.ifPresent(DataScopeContext::set);
        try {
            task.run();
        } finally {
            clear();
            previous.ifPresent(DataScopeContext::set);
        }
    }
}
