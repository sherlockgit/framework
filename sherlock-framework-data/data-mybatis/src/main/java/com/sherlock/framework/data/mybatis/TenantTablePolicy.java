package com.sherlock.framework.data.mybatis;

import com.baomidou.mybatisplus.core.metadata.TableInfo;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class TenantTablePolicy {

    public static final Set<String> PLATFORM_GLOBAL_TABLES = Set.of("flyway_schema_history");

    private final Set<String> globalTables;
    private final Set<String> declaredTenantTables;

    public TenantTablePolicy(Set<String> globalTables, Set<String> declaredTenantTables) {
        this.globalTables = normalize(globalTables, "globalTables");
        this.declaredTenantTables = normalize(declaredTenantTables, "declaredTenantTables");
        if (!this.globalTables.isEmpty() && this.globalTables.stream().anyMatch(this.declaredTenantTables::contains)) {
            throw new IllegalArgumentException("a table cannot be both global and tenant scoped");
        }
    }

    public static TenantTablePolicy defaults() {
        return new TenantTablePolicy(PLATFORM_GLOBAL_TABLES, Set.of());
    }

    public boolean isTenantTable(String tableName) {
        return !globalTables.contains(normalize(tableName));
    }

    public void validateMappedTenantTables(Collection<TableInfo> tableInfos) {
        Objects.requireNonNull(tableInfos, "tableInfos must not be null");
        Map<String, TableInfo> mappings = tableInfos.stream()
                .collect(Collectors.toMap(tableInfo -> normalize(tableInfo.getTableName()), Function.identity(), (left, right) -> left));
        for (Map.Entry<String, TableInfo> mapping : mappings.entrySet()) {
            if (isTenantTable(mapping.getKey()) && !hasTenantIdColumn(mapping.getValue())) {
                throw new IllegalStateException("Tenant table '" + mapping.getKey() + "' must map tenant_id");
            }
        }
        for (String tableName : declaredTenantTables) {
            TableInfo tableInfo = mappings.get(tableName);
            if (tableInfo == null || !hasTenantIdColumn(tableInfo)) {
                throw new IllegalStateException("Tenant table '" + tableName + "' must map tenant_id");
            }
        }
    }

    private static boolean hasTenantIdColumn(TableInfo tableInfo) {
        return "tenant_id".equalsIgnoreCase(tableInfo.getKeyColumn())
                || tableInfo.getFieldList().stream()
                .anyMatch(field -> "tenant_id".equalsIgnoreCase(field.getColumn()));
    }

    private static Set<String> normalize(Set<String> names, String argumentName) {
        Objects.requireNonNull(names, argumentName + " must not be null");
        return names.stream().map(TenantTablePolicy::normalize).collect(Collectors.toUnmodifiableSet());
    }

    private static String normalize(String tableName) {
        Objects.requireNonNull(tableName, "tableName must not be null");
        String normalized = tableName.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("tableName must not be blank");
        }
        return normalized;
    }
}
