package com.sherlock.framework.data.mybatis;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.sherlock.framework.data.scope.DataScope;
import com.sherlock.framework.data.scope.DataScopeContext;
import com.sherlock.framework.security.AuthenticatedPrincipal;
import com.sherlock.framework.security.PrincipalContext;
import com.sherlock.framework.tenant.TenantContext;
import com.sherlock.framework.tenant.TenantId;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TenantAndDataScopeIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.4")
            .withDatabaseName("framework")
            .withUsername("framework")
            .withPassword("framework");

    private SqlSessionFactory sqlSessionFactory;

    @BeforeAll
    void createSchemaAndMapper() throws Exception {
        try (Connection connection = DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    create table orders (
                        id bigint not null auto_increment primary key,
                        tenant_id bigint not null,
                        business_key varchar(64) not null,
                        creator_id bigint not null,
                        department_id bigint not null,
                        name varchar(128) not null,
                        unique key uk_orders_tenant_business_key (tenant_id, business_key)
                    )
                    """);
        }

        var dataSource = new UnpooledDataSource(
                "com.mysql.cj.jdbc.Driver", MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
        var configuration = new MybatisConfiguration(
                new Environment("test", new JdbcTransactionFactory(), dataSource));
        configuration.addInterceptor(interceptor());
        configuration.addMapper(OrderMapper.class);
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
    }

    @BeforeEach
    void seedOrders() throws Exception {
        try (Connection connection = DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("delete from orders");
        }
        insertOrder(1L, "same-business-key", 101L, 10L, "tenant-one");
        insertOrder(2L, "same-business-key", 202L, 20L, "tenant-two");
        insertOrder(1L, "self-order", 101L, 10L, "self-order");
        insertOrder(1L, "department-order", 102L, 11L, "department-order");
    }

    @AfterEach
    void clearContexts() {
        DataScopeContext.clear();
        PrincipalContext.clear();
        TenantContext.clear();
    }

    @Test
    void selectUpdateAndDeleteCannotCrossTenantForTheSameBusinessKey() {
        bind(1L, 101L, DataScope.allTenant());

        assertThat(withMapper(OrderMapper::selectAll)).extracting(OrderRow::name)
                .containsExactly("tenant-one", "self-order", "department-order");
        int updated = withMapper(mapper -> mapper.updateByBusinessKey("same-business-key", "updated-one"));
        assertThat(updated).isEqualTo(1);
        assertThat(rawName(1L, "same-business-key")).isEqualTo("updated-one");
        assertThat(rawName(2L, "same-business-key")).isEqualTo("tenant-two");
        int deleted = withMapper(mapper -> mapper.deleteByBusinessKey("same-business-key"));
        assertThat(deleted).isEqualTo(1);
        assertThat(rawName(1L, "same-business-key")).isNull();
        assertThat(rawName(2L, "same-business-key")).isEqualTo("tenant-two");
    }

    @Test
    void selfDepartmentsAllTenantAndEmptyDepartmentScopesProduceExpectedRows() {
        bind(1L, 101L, DataScope.self());
        assertThat(withMapper(OrderMapper::selectAll)).extracting(OrderRow::name)
                .containsExactly("tenant-one", "self-order");

        DataScopeContext.set(DataScope.departments(Set.of(11L)));
        assertThat(withMapper(OrderMapper::selectAll)).extracting(OrderRow::name)
                .containsExactly("department-order");

        DataScopeContext.set(DataScope.allTenant());
        assertThat(withMapper(OrderMapper::selectAll)).extracting(OrderRow::name)
                .containsExactly("tenant-one", "self-order", "department-order");

        DataScopeContext.set(DataScope.departments(Set.of()));
        assertThat(withMapper(OrderMapper::selectAll)).isEmpty();
    }

    @Test
    void selfAndDepartmentScopesRestrictUpdatesAndDeletesWithinTheCurrentTenant() {
        bind(1L, 101L, DataScope.self());
        int selfUpdate = withMapper(mapper -> mapper.updateByBusinessKey("department-order", "not-allowed"));
        int selfDelete = withMapper(mapper -> mapper.deleteByBusinessKey("department-order"));
        assertThat(selfUpdate).isZero();
        assertThat(selfDelete).isZero();

        DataScopeContext.set(DataScope.departments(Set.of(11L)));
        int departmentUpdate = withMapper(mapper -> mapper.updateByBusinessKey("department-order", "department-updated"));
        int departmentDelete = withMapper(mapper -> mapper.deleteByBusinessKey("self-order"));
        assertThat(departmentUpdate).isEqualTo(1);
        assertThat(departmentDelete).isZero();
        assertThat(rawName(1L, "department-order")).isEqualTo("department-updated");
        assertThat(rawName(1L, "self-order")).isEqualTo("self-order");
    }

    @Test
    void explicitCrossTenantInsertIsRejectedBeforeWritingTheRow() {
        bind(1L, 101L, DataScope.allTenant());

        assertThatThrownBy(() -> withMapper(mapper -> mapper.insertWithTenant(
                2L, "forbidden-insert", 101L, 10L, "forbidden")))
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("Explicit tenant_id writes are forbidden for tenant tables");

        assertThat(rawName(2L, "forbidden-insert")).isNull();
    }

    @Test
    void insertValuesWithoutColumnsCannotWriteAnotherTenant() {
        bind(1L, 101L, DataScope.allTenant());

        assertThatThrownBy(() -> withMapper(mapper -> mapper.insertValuesWithoutColumns(
                "forbidden-values", 101L, 10L, "forbidden")))
                .hasRootCauseInstanceOf(com.sherlock.framework.base.error.BusinessException.class)
                .hasRootCauseMessage("Tenant table inserts must declare target columns");

        assertThat(rawName(2L, "forbidden-values")).isNull();
    }

    @Test
    void insertSelectWithoutColumnsCannotWriteAnotherTenant() {
        bind(1L, 101L, DataScope.allTenant());

        assertThatThrownBy(() -> withMapper(mapper -> mapper.insertSelectWithoutColumns(
                "forbidden-select", 101L, 10L, "forbidden")))
                .hasRootCauseInstanceOf(com.sherlock.framework.base.error.BusinessException.class)
                .hasRootCauseMessage("Tenant table inserts must declare target columns");

        assertThat(rawName(2L, "forbidden-select")).isNull();
    }

    @Test
    void explicitTenantUpdateIsRejectedAndCannotMoveAnExistingRow() {
        bind(1L, 101L, DataScope.allTenant());

        assertThatThrownBy(() -> withMapper(mapper -> mapper.updateTenantByBusinessKey("self-order", 2L)))
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("Explicit tenant_id writes are forbidden for tenant tables");

        assertThat(rawName(1L, "self-order")).isEqualTo("self-order");
        assertThat(rawName(2L, "self-order")).isNull();
    }

    private MybatisPlusInterceptor interceptor() {
        TenantTablePolicy tablePolicy = TenantTablePolicy.defaults();
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new FrameworkTenantLineInnerInterceptor(tablePolicy));
        interceptor.addInnerInterceptor(new DataPermissionInterceptor(new FrameworkDataPermissionHandler(tablePolicy)));
        return interceptor;
    }

    private void bind(long tenantId, long userId, DataScope scope) {
        TenantId tenant = new TenantId(tenantId);
        TenantContext.set(tenant);
        PrincipalContext.set(new AuthenticatedPrincipal(userId, tenant, Set.of(), 0L));
        DataScopeContext.set(scope);
    }

    private void insertOrder(long tenantId, String businessKey, long creatorId, long departmentId, String name) throws Exception {
        try (Connection connection = DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
             PreparedStatement statement = connection.prepareStatement("""
                     insert into orders (tenant_id, business_key, creator_id, department_id, name)
                     values (?, ?, ?, ?, ?)
                     """)) {
            statement.setLong(1, tenantId);
            statement.setString(2, businessKey);
            statement.setLong(3, creatorId);
            statement.setLong(4, departmentId);
            statement.setString(5, name);
            statement.executeUpdate();
        }
    }

    private String rawName(long tenantId, String businessKey) {
        try (Connection connection = DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
             PreparedStatement statement = connection.prepareStatement(
                     "select name from orders where tenant_id = ? and business_key = ?")) {
            statement.setLong(1, tenantId);
            statement.setString(2, businessKey);
            var resultSet = statement.executeQuery();
            return resultSet.next() ? resultSet.getString(1) : null;
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private <T> T withMapper(MapperCall<T> call) {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            return call.call(session.getMapper(OrderMapper.class));
        }
    }

    @FunctionalInterface
    private interface MapperCall<T> {
        T call(OrderMapper mapper);
    }

    interface OrderMapper {
        @Select("""
                select id, tenant_id as tenantId, business_key as businessKey, creator_id as creatorId,
                       department_id as departmentId, name
                from orders order by id
                """)
        List<OrderRow> selectAll();

        @Update("update orders set name = #{name} where business_key = #{businessKey}")
        int updateByBusinessKey(@Param("businessKey") String businessKey, @Param("name") String name);

        @Delete("delete from orders where business_key = #{businessKey}")
        int deleteByBusinessKey(@Param("businessKey") String businessKey);

        @Insert("""
                insert into orders (tenant_id, business_key, creator_id, department_id, name)
                values (#{tenantId}, #{businessKey}, #{creatorId}, #{departmentId}, #{name})
                """)
        int insertWithTenant(
                @Param("tenantId") long tenantId,
                @Param("businessKey") String businessKey,
                @Param("creatorId") long creatorId,
                @Param("departmentId") long departmentId,
                @Param("name") String name);

        @Insert("""
                insert into orders values (null, 2, #{businessKey}, #{creatorId}, #{departmentId}, #{name})
                """)
        int insertValuesWithoutColumns(
                @Param("businessKey") String businessKey,
                @Param("creatorId") long creatorId,
                @Param("departmentId") long departmentId,
                @Param("name") String name);

        @Insert("""
                insert into orders select null, 2, #{businessKey}, #{creatorId}, #{departmentId}, #{name}
                """)
        int insertSelectWithoutColumns(
                @Param("businessKey") String businessKey,
                @Param("creatorId") long creatorId,
                @Param("departmentId") long departmentId,
                @Param("name") String name);

        @Update("update orders set tenant_id = #{tenantId} where business_key = #{businessKey}")
        int updateTenantByBusinessKey(@Param("businessKey") String businessKey, @Param("tenantId") long tenantId);
    }

    record OrderRow(long id, long tenantId, String businessKey, long creatorId, long departmentId, String name) {
    }
}
