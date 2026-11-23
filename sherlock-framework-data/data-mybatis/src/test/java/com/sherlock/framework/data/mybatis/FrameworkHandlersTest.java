package com.sherlock.framework.data.mybatis;

import com.sherlock.framework.data.scope.DataScope;
import com.sherlock.framework.data.scope.DataScopeContext;
import com.sherlock.framework.data.scope.DataScopeType;
import com.sherlock.framework.security.AuthenticatedPrincipal;
import com.sherlock.framework.security.PrincipalContext;
import com.sherlock.framework.tenant.TenantContext;
import com.sherlock.framework.tenant.TenantId;
import net.sf.jsqlparser.expression.LongValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class FrameworkHandlersTest {

    @AfterEach
    void clearContexts() {
        DataScopeContext.clear();
        PrincipalContext.clear();
        TenantContext.clear();
    }

    @Test
    void tenantHandlerUsesRequiredTenantAndTreatsOnlyExplicitGlobalsAsGlobal() {
        TenantContext.set(new TenantId(7L));
        TenantTablePolicy tablePolicy = new TenantTablePolicy(Set.of("platform_settings"), Set.of());
        FrameworkTenantLineHandler handler = new FrameworkTenantLineHandler(tablePolicy);

        assertThat(handler.getTenantId()).isEqualTo(new LongValue(7L));
        assertThat(handler.getTenantIdColumn()).isEqualTo("tenant_id");
        assertThat(handler.ignoreTable("platform_settings")).isTrue();
        assertThat(handler.ignoreTable("orders")).isFalse();
    }

    @Test
    void dataPermissionUsesCurrentUserAndVisibleDepartments() {
        PrincipalContext.set(new AuthenticatedPrincipal(101L, new TenantId(1L), Set.of(), 0L));
        DataScopeContext.set(new DataScope(DataScopeType.DEPARTMENTS, Set.of(10L, 11L), true));
        FrameworkDataPermissionHandler handler = new FrameworkDataPermissionHandler(TenantTablePolicy.defaults());

        assertThat(handler.getSqlSegment(null, "orders.select").toString())
                .isEqualTo("creator_id = 101 OR department_id IN (10, 11)");
    }

    @Test
    void emptyDepartmentScopeProducesAValidDenyingPredicate() {
        PrincipalContext.set(new AuthenticatedPrincipal(101L, new TenantId(1L), Set.of(), 0L));
        DataScopeContext.set(DataScope.departments(Set.of()));
        FrameworkDataPermissionHandler handler = new FrameworkDataPermissionHandler(TenantTablePolicy.defaults());

        assertThat(handler.getSqlSegment(null, "orders.select").toString())
                .isEqualTo("1 = 0 OR 1 = 0");
    }

    @Test
    void allTenantScopeDoesNotAddDataPermissionPredicate() {
        PrincipalContext.set(new AuthenticatedPrincipal(101L, new TenantId(1L), Set.of(), 0L));
        DataScopeContext.set(DataScope.allTenant());
        FrameworkDataPermissionHandler handler = new FrameworkDataPermissionHandler(TenantTablePolicy.defaults());

        assertThat(handler.getSqlSegment(null, "orders.select")).isNull();
    }
}
