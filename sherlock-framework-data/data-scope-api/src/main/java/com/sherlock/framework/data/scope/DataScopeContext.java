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
     * 捕获当前数据范围，用于显式传播至异步或消息处理任务。
     */
    public static Optional<DataScope> snapshot() {
        return current();
    }

    /**
     * 使用显式捕获的数据范围执行任务，并在结束后恢复工作线程原有的数据范围。
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
