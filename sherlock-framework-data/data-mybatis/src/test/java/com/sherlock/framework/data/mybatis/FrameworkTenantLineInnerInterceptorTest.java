package com.sherlock.framework.data.mybatis;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FrameworkTenantLineInnerInterceptorTest {

    private final FrameworkTenantLineInnerInterceptor interceptor =
            new FrameworkTenantLineInnerInterceptor(TenantTablePolicy.defaults());

    @Test
    void rejectsExplicitTenantColumnInInsertValues() {
        assertThatThrownBy(() -> interceptor.parserSingle(
                "insert into orders (tenant_id, business_key) values (2, 'forbidden')", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Explicit tenant_id writes are forbidden for tenant tables");
    }

    @Test
    void rejectsExplicitTenantColumnInInsertSetAndDuplicateUpdate() {
        assertThatThrownBy(() -> interceptor.parserSingle(
                "insert into orders set tenant_id = 2, business_key = 'forbidden'", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Explicit tenant_id writes are forbidden for tenant tables");
        assertThatThrownBy(() -> interceptor.parserSingle(
                "insert into orders (business_key) values ('forbidden') on duplicate key update tenant_id = 2", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Explicit tenant_id writes are forbidden for tenant tables");
    }

    @Test
    void rejectsExplicitTenantColumnInUpdate() {
        assertThatThrownBy(() -> interceptor.parserSingle(
                "update orders set tenant_id = 2 where business_key = 'forbidden'", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Explicit tenant_id writes are forbidden for tenant tables");
    }
}
