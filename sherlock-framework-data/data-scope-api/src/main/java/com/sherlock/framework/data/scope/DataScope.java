package com.sherlock.framework.data.scope;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public record DataScope(DataScopeType type, Set<Long> visibleDepartmentIds, boolean includesSelf) {

    public DataScope {
        type = Objects.requireNonNull(type, "type must not be null");
        visibleDepartmentIds = immutableDepartmentIds(visibleDepartmentIds);
        if (type == DataScopeType.ALL_TENANT && (!visibleDepartmentIds.isEmpty() || includesSelf)) {
            throw new IllegalArgumentException("ALL_TENANT scope cannot restrict users or departments");
        }
        if (type == DataScopeType.SELF && (!visibleDepartmentIds.isEmpty() || !includesSelf)) {
            throw new IllegalArgumentException("SELF scope must include only the current user");
        }
    }

    public static DataScope self() {
        return new DataScope(DataScopeType.SELF, Set.of(), true);
    }

    public static DataScope departments(Set<Long> departmentIds) {
        return new DataScope(DataScopeType.DEPARTMENTS, departmentIds, false);
    }

    public static DataScope managedDepartments(Set<Long> departmentIds) {
        return new DataScope(DataScopeType.MANAGED_DEPARTMENTS, departmentIds, false);
    }

    public static DataScope allTenant() {
        return new DataScope(DataScopeType.ALL_TENANT, Set.of(), false);
    }

    public static DataScope merge(Collection<DataScope> scopes) {
        Objects.requireNonNull(scopes, "scopes must not be null");
        if (scopes.isEmpty()) {
            throw new IllegalArgumentException("scopes must not be empty");
        }

        boolean includesSelf = false;
        boolean managedDepartments = false;
        Set<Long> departmentIds = new LinkedHashSet<>();
        for (DataScope scope : scopes) {
            Objects.requireNonNull(scope, "scope must not be null");
            if (scope.type == DataScopeType.ALL_TENANT) {
                return allTenant();
            }
            includesSelf |= scope.includesSelf;
            managedDepartments |= scope.type == DataScopeType.MANAGED_DEPARTMENTS;
            departmentIds.addAll(scope.visibleDepartmentIds);
        }

        if (departmentIds.isEmpty()) {
            if (includesSelf) {
                return self();
            }
            return new DataScope(
                    managedDepartments ? DataScopeType.MANAGED_DEPARTMENTS : DataScopeType.DEPARTMENTS,
                    Set.of(),
                    false);
        }
        return new DataScope(
                managedDepartments ? DataScopeType.MANAGED_DEPARTMENTS : DataScopeType.DEPARTMENTS,
                departmentIds,
                includesSelf);
    }

    public boolean isAllTenant() {
        return type == DataScopeType.ALL_TENANT;
    }

    private static Set<Long> immutableDepartmentIds(Set<Long> departmentIds) {
        Objects.requireNonNull(departmentIds, "visibleDepartmentIds must not be null");
        if (departmentIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("department IDs must be positive");
        }
        return Set.copyOf(departmentIds);
    }
}
