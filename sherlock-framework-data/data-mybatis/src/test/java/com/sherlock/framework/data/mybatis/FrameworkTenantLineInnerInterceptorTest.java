package com.sherlock.framework.data.mybatis;

import com.sherlock.framework.base.error.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
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
    void rejectsTenantTableInsertSetBeforeExaminingExplicitTenantColumn() {
        assertThatThrownBy(() -> interceptor.parserSingle(
                "insert into orders set tenant_id = 2, business_key = 'forbidden'", null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Tenant table inserts must declare target columns")
                .extracting(exception -> ((BusinessException) exception).errorCode().value())
                .isEqualTo("DATA-400-001");
    }

    @Test
    void rejectsExplicitTenantColumnInInsertDuplicateUpdate() {
        assertThatThrownBy(() -> interceptor.parserSingle(
                "insert into orders (business_key) values ('forbidden') on duplicate key update tenant_id = 2", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Explicit tenant_id writes are forbidden for tenant tables");
    }

    @Test
    void rejectsTenantTableInsertValuesWithoutAnExplicitColumnList() {
        assertThatThrownBy(() -> interceptor.parserSingle(
                "insert into orders values (1, 2, 'forbidden')", null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Tenant table inserts must declare target columns")
                .extracting(exception -> ((BusinessException) exception).errorCode().value())
                .isEqualTo("DATA-400-001");
    }

    @Test
    void rejectsTenantTableInsertSelectWithoutAnExplicitColumnList() {
        assertThatThrownBy(() -> interceptor.parserSingle(
                "insert into orders select 1, 2, 'forbidden'", null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Tenant table inserts must declare target columns")
                .extracting(exception -> ((BusinessException) exception).errorCode().value())
                .isEqualTo("DATA-400-001");
    }

    @Test
    void rejectsTenantTableInsertSetWithoutAnExplicitColumnList() {
        assertThatThrownBy(() -> interceptor.parserSingle(
                "insert into orders set business_key = 'forbidden'", null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Tenant table inserts must declare target columns")
                .extracting(exception -> ((BusinessException) exception).errorCode().value())
                .isEqualTo("DATA-400-001");
    }

    @Test
    void allowsGlobalTableInsertWithoutAnExplicitColumnList() {
        assertThatCode(() -> interceptor.parserSingle(
                "insert into flyway_schema_history values (1, 'global')", null))
                .doesNotThrowAnyException();
    }

    @Test
    void allowsGlobalTableInsertSetWithoutAnExplicitColumnList() {
        assertThatCode(() -> interceptor.parserSingle(
                "insert into flyway_schema_history set installed_rank = 1, version = 'global'", null))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsExplicitTenantColumnInUpdate() {
        assertThatThrownBy(() -> interceptor.parserSingle(
                "update orders set tenant_id = 2 where business_key = 'forbidden'", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Explicit tenant_id writes are forbidden for tenant tables");
    }
}
