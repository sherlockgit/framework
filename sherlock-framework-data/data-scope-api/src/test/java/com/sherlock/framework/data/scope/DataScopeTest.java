package com.sherlock.framework.data.scope;

import com.sherlock.framework.security.AuthenticatedPrincipal;
import com.sherlock.framework.security.PrincipalContext;
import com.sherlock.framework.tenant.TenantId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DataScopeTest {

    @AfterEach
    void clearContext() {
        DataScopeContext.clear();
        PrincipalContext.clear();
    }

    @Test
    void roleScopesMergeByUnionAndAllTenantDominates() {
        DataScope result = DataScope.merge(List.of(
                DataScope.self(),
                DataScope.departments(Set.of(10L, 11L)),
                DataScope.managedDepartments(Set.of(12L)),
                DataScope.allTenant()));

        assertThat(result.type()).isEqualTo(DataScopeType.ALL_TENANT);
        assertThat(result.visibleDepartmentIds()).isEmpty();
        assertThat(result.includesSelf()).isFalse();
    }

    @Test
    void roleScopesKeepSelfAndUnionDepartmentIdsWhenNotAllTenant() {
        DataScope result = DataScope.merge(List.of(
                DataScope.self(),
                DataScope.departments(Set.of(10L, 11L)),
                DataScope.managedDepartments(Set.of(11L, 12L))));

        assertThat(result.type()).isEqualTo(DataScopeType.MANAGED_DEPARTMENTS);
        assertThat(result.includesSelf()).isTrue();
        assertThat(result.visibleDepartmentIds()).containsExactlyInAnyOrder(10L, 11L, 12L);
    }

    @Test
    void contextRequiresExplicitScope() {
        assertThatThrownBy(DataScopeContext::require)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void emptyDepartmentScopeDoesNotAccidentallyGrantSelfAccessWhenMerged() {
        DataScope result = DataScope.merge(List.of(DataScope.departments(Set.of())));

        assertThat(result.type()).isEqualTo(DataScopeType.DEPARTMENTS);
        assertThat(result.includesSelf()).isFalse();
        assertThat(result.visibleDepartmentIds()).isEmpty();
    }

    @Test
    void contextKeepsExplicitScopeForTests() {
        DataScope scope = DataScope.departments(Set.of(10L));

        DataScopeContext.set(scope);

        assertThat(DataScopeContext.current()).contains(scope);
    }

    @Test
    void resolverBuildsAndBindsScopeFromCurrentPrincipalAndPolicySnapshot() {
        PrincipalContext.set(new AuthenticatedPrincipal(101L, new TenantId(1L), Set.of(), 7L));
        DataScope expected = DataScope.departments(Set.of(10L));
        DataScopeResolver resolver = new DataScopeResolver(principal -> {
            assertThat(principal.userId()).isEqualTo(101L);
            assertThat(principal.policyVersion()).isEqualTo(7L);
            return expected;
        });

        resolver.resolveAndBind();

        assertThat(DataScopeContext.require()).isEqualTo(expected);
    }

    @Test
    void snapshotAndExecuteWithTransferScopeWithoutLeakingIntoWorkerThread() {
        DataScope capturedScope = DataScope.departments(Set.of(10L));
        DataScope workerScope = DataScope.departments(Set.of(99L));
        DataScopeContext.set(capturedScope);

        Optional<DataScope> snapshot = DataScopeContext.snapshot();
        DataScopeContext.set(workerScope);

        DataScopeContext.executeWith(snapshot, () -> assertThat(DataScopeContext.require()).isEqualTo(capturedScope));

        assertThat(DataScopeContext.require()).isEqualTo(workerScope);
    }

    @Test
    void executeWithRestoresWorkerScopeWhenTheTaskFails() {
        DataScope workerScope = DataScope.departments(Set.of(99L));
        DataScopeContext.set(workerScope);

        assertThatThrownBy(() -> DataScopeContext.executeWith(Optional.of(DataScope.self()), () -> {
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class).hasMessage("boom");

        assertThat(DataScopeContext.require()).isEqualTo(workerScope);
    }
}
