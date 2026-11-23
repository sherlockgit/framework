# Sherlock Framework 第一阶段实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**目标：** 建立可发布的 Maven 框架骨架，并交付能在单个 Spring Boot 服务中验证的租户上下文、JWT 认证、共享表租户隔离、部门数据范围与 Redis 租户缓存键能力。

**架构：** `base`、`tenant-api`、`security-api` 与 `data-scope-api` 保持纯 Java；每种 Spring 或中间件集成由独立 Starter 提供。Web 过滤器建立认证和租户上下文，MyBatis-Plus 拦截器统一注入 `tenant_id` 及数据范围，Redis 缓存键按租户分区。

**技术栈：** Java 21、Maven、Spring Boot 3.x、Spring Security、Nimbus JOSE + JWT、MyBatis-Plus、MySQL 8、Redis、Testcontainers、JUnit 5、AssertJ。

---

## 范围与文件结构

本计划只覆盖设计文档中的交付序列第 1、2 步。Nacos、Gateway、RabbitMQ、Outbox、审计服务和可观测性属于后续独立计划。

```text
pom.xml                                             聚合父工程与质量插件
sherlock-framework-bom/pom.xml                      依赖版本与 BOM 导入
sherlock-framework-base/                            纯 Java 错误、结果与分页模型
sherlock-framework-tenant/tenant-api/               租户上下文和传播契约
sherlock-framework-tenant/tenant-spring-boot-starter/ Servlet 过滤器与自动装配
sherlock-framework-security/security-api/           认证主体与授权契约
sherlock-framework-security/security-jwt/           JWT 签发、验证、刷新令牌契约
sherlock-framework-security/security-spring-boot-starter/ Spring Security 资源服务配置
sherlock-framework-web/web-core/                    错误响应、全局异常映射
sherlock-framework-web/web-spring-boot-starter/     Web MVC 自动装配
sherlock-framework-data/data-scope-api/             数据范围模型与解析契约
sherlock-framework-data/data-mybatis/               MyBatis-Plus 租户与数据范围拦截器
sherlock-framework-data/data-spring-boot-starter/   数据层自动装配和启动校验
sherlock-framework-cache/cache-api/                 缓存键与缓存抽象
sherlock-framework-cache/cache-redis/               Redis 实现与租户键命名空间
sherlock-framework-cache/cache-redis-spring-boot-starter/ Redis 自动装配
examples/tenant-demo/                               验收用最小业务服务
```

根 POM 使用受控属性管理所有第三方版本，模块 POM 不得声明版本。实现前必须在 BOM 中选择并验证以下兼容组合：Java 21、Spring Boot 3.x、Spring Security、MyBatis-Plus、MySQL JDBC、Redis 客户端、Testcontainers 与 Nimbus JOSE + JWT。`mvn -q dependency:tree` 的结果必须保存为 CI 构件，且 `maven-enforcer-plugin` 必须拒绝 Java 低于 21 和依赖版本漂移。

### Task 1：建立 Maven 聚合、BOM 与构建门槛

**文件：**
- Create: `pom.xml`
- Create: `sherlock-framework-bom/pom.xml`
- Create: `sherlock-framework-base/pom.xml`
- Create: `.mvn/jvm.config`
- Create: `.github/workflows/verify.yml`
- Test: `pom.xml`

- [ ] **步骤 1：写出会失败的聚合验证命令。**

运行：

```powershell
mvn -q -DskipTests verify
```

预期：失败，提示仓库根目录没有 `pom.xml`。

- [ ] **步骤 2：创建根 POM、BOM 和 Java 21 强制规则。**

根 POM 必须列出本计划中的全部聚合模块，设置：

```xml
<properties>
  <maven.compiler.release>21</maven.compiler.release>
  <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
</properties>
<build>
  <pluginManagement>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <configuration><release>${maven.compiler.release}</release></configuration>
      </plugin>
    </plugins>
  </pluginManagement>
</build>
```

使用 `maven-enforcer-plugin` 的 `requireJavaVersion` 规则要求 `[21,)`，使用 `requireUpperBoundDeps` 拒绝传递依赖版本冲突。BOM 使用 `<dependencyManagement>` 导入 Spring Boot BOM，并集中管理 MyBatis-Plus、Nimbus、Testcontainers 的版本。

- [ ] **步骤 3：创建基础模块 POM 和 JVM 参数文件。**

`sherlock-framework-base/pom.xml` 仅依赖 JUnit Jupiter 与 AssertJ 的测试作用域；不得引入 Spring。`.mvn/jvm.config` 内容为：

```text
-Dfile.encoding=UTF-8
```

- [ ] **步骤 4：写最小 CI 验证。**

创建 `.github/workflows/verify.yml`，使用 Java 21，执行：

```yaml
- run: mvn --batch-mode --update-snapshots verify
- run: mvn --batch-mode -q dependency:tree -DoutputFile=target/dependency-tree.txt
```

- [ ] **步骤 5：运行构建并确认成功。**

运行：

```powershell
mvn --batch-mode verify
```

预期：退出码为 `0`，Enforcer 显示 Java 21 校验通过。

- [ ] **步骤 6：提交工程骨架。**

```powershell
git add pom.xml .mvn .github sherlock-framework-bom sherlock-framework-base
git commit -m "build: initialize Maven framework modules"
```

### Task 2：实现纯 Java 基础错误、结果与分页模型

**文件：**
- Create: `sherlock-framework-base/src/main/java/com/sherlock/framework/base/error/ErrorCode.java`
- Create: `sherlock-framework-base/src/main/java/com/sherlock/framework/base/error/BusinessException.java`
- Create: `sherlock-framework-base/src/main/java/com/sherlock/framework/base/web/ApiResponse.java`
- Create: `sherlock-framework-base/src/main/java/com/sherlock/framework/base/page/PageRequest.java`
- Create: `sherlock-framework-base/src/main/java/com/sherlock/framework/base/page/PageResult.java`
- Test: `sherlock-framework-base/src/test/java/com/sherlock/framework/base/ApiResponseTest.java`
- Test: `sherlock-framework-base/src/test/java/com/sherlock/framework/base/PageRequestTest.java`

- [ ] **步骤 1：写失败测试。**

```java
@Test
void failureResponseKeepsStableCodeAndTraceId() {
    var code = new ErrorCode("AUTH-401-001", "认证已失效");
    var response = ApiResponse.failure(code, "trace-123");

    assertThat(response.success()).isFalse();
    assertThat(response.code()).isEqualTo("AUTH-401-001");
    assertThat(response.message()).isEqualTo("认证已失效");
    assertThat(response.traceId()).isEqualTo("trace-123");
}

@Test
void pageRequestRejectsNonPositivePageNumber() {
    assertThatThrownBy(() -> new PageRequest(0, 20))
        .isInstanceOf(IllegalArgumentException.class);
}
```

- [ ] **步骤 2：运行测试确认失败。**

运行：

```powershell
mvn -pl sherlock-framework-base test
```

预期：编译失败，提示 `ErrorCode`、`ApiResponse` 和 `PageRequest` 不存在。

- [ ] **步骤 3：写最小实现。**

```java
public record ErrorCode(String value, String defaultMessage) {
    public ErrorCode {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("错误码不能为空");
    }
}

public record ApiResponse<T>(boolean success, String code, String message, T data, String traceId) {
    public static <T> ApiResponse<T> success(T data, String traceId) {
        return new ApiResponse<>(true, "OK", "OK", data, traceId);
    }
    public static <T> ApiResponse<T> failure(ErrorCode code, String traceId) {
        return new ApiResponse<>(false, code.value(), code.defaultMessage(), null, traceId);
    }
}

public record PageRequest(int pageNumber, int pageSize) {
    public PageRequest {
        if (pageNumber < 1 || pageSize < 1 || pageSize > 500) throw new IllegalArgumentException("分页参数无效");
    }
}
```

`BusinessException` 必须保存 `ErrorCode`，`PageResult<T>` 必须保存 `List<T> records`、`long total`、`int pageNumber` 和 `int pageSize`，并拒绝负数总数。

- [ ] **步骤 4：运行基础模块测试。**

运行：

```powershell
mvn -pl sherlock-framework-base test
```

预期：退出码为 `0`，两个测试全部通过。

- [ ] **步骤 5：提交基础契约。**

```powershell
git add sherlock-framework-base
git commit -m "feat: add base error response and paging models"
```

### Task 3：实现租户上下文及 Servlet 自动装配

**文件：**
- Create: `sherlock-framework-tenant/tenant-api/pom.xml`
- Create: `sherlock-framework-tenant/tenant-api/src/main/java/com/sherlock/framework/tenant/TenantId.java`
- Create: `sherlock-framework-tenant/tenant-api/src/main/java/com/sherlock/framework/tenant/TenantContext.java`
- Create: `sherlock-framework-tenant/tenant-api/src/main/java/com/sherlock/framework/tenant/TenantContextSnapshot.java`
- Create: `sherlock-framework-tenant/tenant-spring-boot-starter/pom.xml`
- Create: `sherlock-framework-tenant/tenant-spring-boot-starter/src/main/java/com/sherlock/framework/tenant/autoconfigure/TenantAutoConfiguration.java`
- Create: `sherlock-framework-tenant/tenant-spring-boot-starter/src/main/java/com/sherlock/framework/tenant/web/TenantContextCleanupFilter.java`
- Create: `sherlock-framework-tenant/tenant-spring-boot-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Test: `sherlock-framework-tenant/tenant-api/src/test/java/com/sherlock/framework/tenant/TenantContextTest.java`
- Test: `sherlock-framework-tenant/tenant-spring-boot-starter/src/test/java/com/sherlock/framework/tenant/web/TenantContextCleanupFilterTest.java`

- [ ] **步骤 1：写失败测试，验证嵌套绑定和清理。**

```java
@Test
void executeWithRestoresPreviousTenant() {
    TenantContext.set(new TenantId(10L));

    TenantContext.executeWith(new TenantId(20L), () ->
        assertThat(TenantContext.require().value()).isEqualTo(20L));

    assertThat(TenantContext.require().value()).isEqualTo(10L);
}

@Test
void filterClearsContextWhenChainThrows() throws Exception {
    var filter = new TenantContextCleanupFilter();
    assertThatThrownBy(() -> filter.doFilter(request, response, (req, res) -> {
        TenantContext.set(new TenantId(1L));
        throw new IllegalStateException("boom");
    })).isInstanceOf(IllegalStateException.class);
    assertThat(TenantContext.current()).isEmpty();
}
```

- [ ] **步骤 2：运行测试确认失败。**

```powershell
mvn -pl sherlock-framework-tenant/tenant-api,sherlock-framework-tenant/tenant-spring-boot-starter test
```

预期：编译失败，提示租户类型和过滤器不存在。

- [ ] **步骤 3：实现上下文及自动装配。**

`TenantId` 为正 `long` 的 record。`TenantContext` 提供 `current()`、`require()`、`set(TenantId)`、`clear()` 与 `executeWith(TenantId, Runnable)`；`require()` 在缺少租户时抛出 `BusinessException`，错误码为 `TENANT-400-001`。

过滤器必须使用 `try/finally`：

```java
try {
    filterChain.doFilter(request, response);
} finally {
    TenantContext.clear();
}
```

自动装配仅在 Servlet Web 应用存在时注册 `TenantContextCleanupFilter`，并写入 `AutoConfiguration.imports`。

- [ ] **步骤 4：运行租户模块测试。**

```powershell
mvn -pl sherlock-framework-tenant/tenant-api,sherlock-framework-tenant/tenant-spring-boot-starter test
```

预期：退出码为 `0`，异常路径后 `TenantContext.current()` 为空。

- [ ] **步骤 5：提交租户上下文。**

```powershell
git add sherlock-framework-tenant pom.xml
git commit -m "feat: add tenant context and web cleanup starter"
```

### Task 4：实现认证主体、JWT 与 Spring Security 资源服务

**文件：**
- Create: `sherlock-framework-security/security-api/src/main/java/com/sherlock/framework/security/AuthenticatedPrincipal.java`
- Create: `sherlock-framework-security/security-api/src/main/java/com/sherlock/framework/security/PrincipalContext.java`
- Create: `sherlock-framework-security/security-api/src/main/java/com/sherlock/framework/security/Permission.java`
- Create: `sherlock-framework-security/security-jwt/src/main/java/com/sherlock/framework/security/jwt/JwtClaims.java`
- Create: `sherlock-framework-security/security-jwt/src/main/java/com/sherlock/framework/security/jwt/JwtTokenService.java`
- Create: `sherlock-framework-security/security-jwt/src/main/java/com/sherlock/framework/security/jwt/NimbusJwtTokenService.java`
- Create: `sherlock-framework-security/security-spring-boot-starter/src/main/java/com/sherlock/framework/security/autoconfigure/SecurityAutoConfiguration.java`
- Create: `sherlock-framework-security/security-spring-boot-starter/src/main/java/com/sherlock/framework/security/web/PrincipalContextFilter.java`
- Test: `sherlock-framework-security/security-jwt/src/test/java/com/sherlock/framework/security/jwt/NimbusJwtTokenServiceTest.java`
- Test: `sherlock-framework-security/security-spring-boot-starter/src/test/java/com/sherlock/framework/security/web/PrincipalContextFilterTest.java`

- [ ] **步骤 1：写 JWT 往返失败测试。**

```java
@Test
void issuedTokenContainsTenantAndPolicyVersion() {
    var service = tokenServiceWithTestKey();
    var claims = new JwtClaims(7L, new TenantId(9L), Set.of("order:read"), 3L, "jti-1");

    var token = service.issue(claims, Duration.ofMinutes(15));
    var parsed = service.verify(token);

    assertThat(parsed.userId()).isEqualTo(7L);
    assertThat(parsed.tenantId().value()).isEqualTo(9L);
    assertThat(parsed.policyVersion()).isEqualTo(3L);
}
```

- [ ] **步骤 2：运行 JWT 测试确认失败。**

```powershell
mvn -pl sherlock-framework-security/security-jwt test
```

预期：编译失败，提示 JWT 服务和声明类型不存在。

- [ ] **步骤 3：实现最小 JWT 服务和安全上下文。**

`JwtClaims` 只包含 `userId`、`TenantId tenantId`、`Set<String> permissions`、`long policyVersion`、`String tokenId`。`NimbusJwtTokenService` 以 RS256 签发并验证令牌，必须校验签名、过期时间、签发者和受众；不得将部门树放入令牌。

`AuthenticatedPrincipal` 必须保存用户、租户、权限集合和策略版本。`PrincipalContext` 提供 `current()`、`require()`、`set(AuthenticatedPrincipal)` 和 `clear()`，其实现与 `TenantContext` 相同，均用 `ThreadLocal` 且不得共享可变对象。`PrincipalContextFilter` 从 Spring Security 已认证对象读取主体，建立上下文，并用 `finally` 清理。未认证请求由 Spring Security 返回 401；已认证但权限不足由授权规则返回 403。

- [ ] **步骤 4：运行 JWT 和安全 Starter 测试。**

```powershell
mvn -pl sherlock-framework-security/security-jwt,sherlock-framework-security/security-spring-boot-starter test
```

预期：退出码为 `0`；过期令牌、错误受众和签名篡改令牌均被拒绝。

- [ ] **步骤 5：提交认证能力。**

```powershell
git add sherlock-framework-security pom.xml
git commit -m "feat: add JWT security contracts and starter"
```

### Task 5：实现统一 Web 错误响应

**文件：**
- Create: `sherlock-framework-web/web-core/src/main/java/com/sherlock/framework/web/error/FrameworkErrorHandler.java`
- Create: `sherlock-framework-web/web-core/src/main/java/com/sherlock/framework/web/trace/TraceIdResolver.java`
- Create: `sherlock-framework-web/web-spring-boot-starter/src/main/java/com/sherlock/framework/web/autoconfigure/WebAutoConfiguration.java`
- Test: `sherlock-framework-web/web-spring-boot-starter/src/test/java/com/sherlock/framework/web/FrameworkErrorHandlerMvcTest.java`

- [ ] **步骤 1：写失败的 MockMvc 测试。**

```java
@Test
void businessExceptionReturnsStableJsonWithoutStackTrace() throws Exception {
    mvc.perform(get("/test/business-error").header("X-Trace-Id", "trace-9"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("TENANT-400-001"))
        .andExpect(jsonPath("$.traceId").value("trace-9"))
        .andExpect(jsonPath("$.stackTrace").doesNotExist());
}
```

- [ ] **步骤 2：运行测试确认失败。**

```powershell
mvn -pl sherlock-framework-web/web-spring-boot-starter test
```

预期：测试上下文无法建立，异常处理器不存在。

- [ ] **步骤 3：实现异常映射和追踪 ID 解析。**

`TraceIdResolver` 优先使用符合 `[A-Za-z0-9-]{1,64}` 的 `X-Trace-Id`，否则生成 UUID。`FrameworkErrorHandler` 将 `BusinessException` 映射为 400，将 `AccessDeniedException` 映射为 403，将 `AuthenticationException` 映射为 401，将未处理异常映射为 `SYS-500-001` 和 500。所有响应使用 `ApiResponse`，未处理异常不序列化异常信息。

- [ ] **步骤 4：运行 Web Starter 测试。**

```powershell
mvn -pl sherlock-framework-web/web-spring-boot-starter test
```

预期：退出码为 `0`，响应只包含契约字段。

- [ ] **步骤 5：提交 Web 错误契约。**

```powershell
git add sherlock-framework-web pom.xml
git commit -m "feat: add web error handling starter"
```

### Task 6：实现数据范围模型和 MyBatis 共享表隔离

**文件：**
- Create: `sherlock-framework-data/data-scope-api/src/main/java/com/sherlock/framework/data/scope/DataScopeType.java`
- Create: `sherlock-framework-data/data-scope-api/src/main/java/com/sherlock/framework/data/scope/DataScope.java`
- Create: `sherlock-framework-data/data-scope-api/src/main/java/com/sherlock/framework/data/scope/DataScopeResolver.java`
- Create: `sherlock-framework-data/data-scope-api/src/main/java/com/sherlock/framework/data/scope/DataScopeContext.java`
- Create: `sherlock-framework-data/data-mybatis/src/main/java/com/sherlock/framework/data/mybatis/TenantTablePolicy.java`
- Create: `sherlock-framework-data/data-mybatis/src/main/java/com/sherlock/framework/data/mybatis/FrameworkTenantLineHandler.java`
- Create: `sherlock-framework-data/data-mybatis/src/main/java/com/sherlock/framework/data/mybatis/FrameworkDataPermissionHandler.java`
- Create: `sherlock-framework-data/data-spring-boot-starter/src/main/java/com/sherlock/framework/data/autoconfigure/DataAutoConfiguration.java`
- Test: `sherlock-framework-data/data-scope-api/src/test/java/com/sherlock/framework/data/scope/DataScopeTest.java`
- Test: `sherlock-framework-data/data-mybatis/src/test/java/com/sherlock/framework/data/mybatis/TenantAndDataScopeIntegrationTest.java`

- [ ] **步骤 1：写失败的范围合并和 SQL 集成测试。**

```java
@Test
void roleScopesMergeByUnionAndAllTenantDominates() {
    var result = DataScope.merge(List.of(
        DataScope.self(),
        DataScope.departments(Set.of(10L, 11L)),
        DataScope.allTenant()));

    assertThat(result.type()).isEqualTo(DataScopeType.ALL_TENANT);
}

@Test
void selectCannotReadAnotherTenantRow() {
    insertOrder(1L, 101L, 10L, "tenant-one");
    insertOrder(2L, 202L, 20L, "tenant-two");
    TenantContext.set(new TenantId(1L));
    DataScopeContext.set(DataScope.allTenant());

    assertThat(orderMapper.selectAll()).extracting(OrderRow::name)
        .containsExactly("tenant-one");
}
```

- [ ] **步骤 2：运行测试确认失败。**

```powershell
mvn -pl sherlock-framework-data/data-scope-api,sherlock-framework-data/data-mybatis test
```

预期：编译失败，数据范围和拦截器类型不存在。

- [ ] **步骤 3：实现范围合并与 MyBatis-Plus 拦截器。**

`DataScopeType` 取值固定为 `SELF`、`DEPARTMENTS`、`MANAGED_DEPARTMENTS`、`ALL_TENANT`。`DataScope.merge` 在任一角色拥有 `ALL_TENANT` 时返回 `ALL_TENANT`，否则合并部门 ID 并保留 `SELF`。`DataScopeContext` 提供 `current()`、`require()`、`set(DataScope)` 与 `clear()`；生产环境中的 `DataScopeResolver` 从 `PrincipalContext` 和策略快照建立范围，集成测试直接设置 `DataScopeContext`。

Starter 注册一个 `MybatisPlusInterceptor`，其中 `TenantLineInnerInterceptor` 的租户列固定为 `tenant_id`，租户值来自 `TenantContext.require()`。`FrameworkDataPermissionHandler` 在查询中追加：

```sql
AND (
  creator_id = :currentUserId
  OR department_id IN (:visibleDepartmentIds)
)
```

仅当数据范围为 `ALL_TENANT` 时省略第二个括号；不得省略租户条件。`TenantTablePolicy` 必须显式列出平台全局表，其他表默认视为租户表。配置声明为租户表但未映射 `tenant_id` 时应用启动失败。

- [ ] **步骤 4：使用 MySQL Testcontainers 运行真实 SQL 测试。**

```powershell
mvn -pl sherlock-framework-data/data-mybatis test -Dtest=TenantAndDataScopeIntegrationTest
```

预期：退出码为 `0`；两个租户具有相同业务主键时，任何查询、更新和删除均只影响当前租户数据。

- [ ] **步骤 5：提交数据隔离实现。**

```powershell
git add sherlock-framework-data pom.xml
git commit -m "feat: add tenant and data scope MyBatis enforcement"
```

### Task 7：实现 Redis 租户缓存键和最小缓存适配器

**文件：**
- Create: `sherlock-framework-cache/cache-api/src/main/java/com/sherlock/framework/cache/CacheKey.java`
- Create: `sherlock-framework-cache/cache-api/src/main/java/com/sherlock/framework/cache/CachePort.java`
- Create: `sherlock-framework-cache/cache-redis/src/main/java/com/sherlock/framework/cache/redis/TenantCacheKeyFactory.java`
- Create: `sherlock-framework-cache/cache-redis/src/main/java/com/sherlock/framework/cache/redis/RedisCachePort.java`
- Create: `sherlock-framework-cache/cache-redis-spring-boot-starter/src/main/java/com/sherlock/framework/cache/autoconfigure/RedisCacheAutoConfiguration.java`
- Test: `sherlock-framework-cache/cache-redis/src/test/java/com/sherlock/framework/cache/redis/TenantCacheKeyFactoryTest.java`
- Test: `sherlock-framework-cache/cache-redis/src/test/java/com/sherlock/framework/cache/redis/RedisCachePortIntegrationTest.java`

- [ ] **步骤 1：写失败测试。**

```java
@Test
void keyAlwaysIncludesCurrentTenant() {
    TenantContext.set(new TenantId(42L));
    assertThat(factory.create("order", "1001").value())
        .isEqualTo("order:42:1001");
}

@Test
void missingTenantRejectsCacheAccess() {
    TenantContext.clear();
    assertThatThrownBy(() -> factory.create("order", "1001"))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("租户");
}
```

- [ ] **步骤 2：运行测试确认失败。**

```powershell
mvn -pl sherlock-framework-cache/cache-redis test
```

预期：编译失败，缓存接口和键工厂不存在。

- [ ] **步骤 3：实现租户键和 Redis 适配器。**

`CacheKey` 不接受空片段、冒号或空白。`TenantCacheKeyFactory.create(namespace, resource)` 必须调用 `TenantContext.require()` 并产生 `{namespace}:{tenantId}:{resource}`。`RedisCachePort` 只提供 `get`、`put`、`evict`，每次调用均通过工厂构造键，不接受原始 Redis 键。

- [ ] **步骤 4：用 Redis Testcontainers 验证隔离。**

```powershell
mvn -pl sherlock-framework-cache/cache-redis test -Dtest=RedisCachePortIntegrationTest
```

预期：退出码为 `0`；租户 1 写入的同资源键无法被租户 2 读取。

- [ ] **步骤 5：提交缓存隔离实现。**

```powershell
git add sherlock-framework-cache pom.xml
git commit -m "feat: add tenant safe Redis cache adapter"
```

### Task 8：交付最小租户验收服务与端到端回归

**文件：**
- Create: `examples/tenant-demo/pom.xml`
- Create: `examples/tenant-demo/src/main/java/com/sherlock/framework/example/TenantDemoApplication.java`
- Create: `examples/tenant-demo/src/main/java/com/sherlock/framework/example/order/OrderRow.java`
- Create: `examples/tenant-demo/src/main/java/com/sherlock/framework/example/order/OrderMapper.java`
- Create: `examples/tenant-demo/src/main/java/com/sherlock/framework/example/order/OrderController.java`
- Create: `examples/tenant-demo/src/main/resources/db/migration/V1__create_orders.sql`
- Create: `examples/tenant-demo/src/test/java/com/sherlock/framework/example/TenantIsolationE2ETest.java`
- Modify: `README.md`

- [ ] **步骤 1：写端到端失败测试。**

```java
@Test
void tenantOneCannotReadTenantTwoOrderThroughHttpOrCache() {
    var tenantOneToken = tokenFor(1L, 101L, Set.of("order:read"));
    var tenantTwoToken = tokenFor(2L, 201L, Set.of("order:read"));
    createOrder(tenantOneToken, "same-business-key", "one");
    createOrder(tenantTwoToken, "same-business-key", "two");

    getOrder(tenantOneToken, "same-business-key").jsonPath("$.data.name").isEqualTo("one");
    getOrder(tenantTwoToken, "same-business-key").jsonPath("$.data.name").isEqualTo("two");
}
```

- [ ] **步骤 2：运行端到端测试确认失败。**

```powershell
mvn -pl examples/tenant-demo test -Dtest=TenantIsolationE2ETest
```

预期：编译失败，示例服务与测试夹具不存在。

- [ ] **步骤 3：实现最小示例服务。**

Flyway 脚本必须创建 `orders` 表，包含 `id`、`tenant_id`、`business_key`、`creator_id`、`department_id`、`name`、`created_at`，以及唯一索引 `uk_orders_tenant_business_key (tenant_id, business_key)`。控制器只暴露创建订单和按业务键读取订单两个端点；所有响应使用 `ApiResponse`。

README 必须给出 Java 21 前置条件、启动 MySQL 和 Redis 测试容器的命令、执行 `mvn verify` 的命令，以及两租户隔离的验收场景。

- [ ] **步骤 4：运行完整回归。**

```powershell
mvn --batch-mode verify
```

预期：退出码为 `0`；端到端测试验证 HTTP 查询、MyBatis 查询和 Redis 缓存三层均隔离租户数据。

- [ ] **步骤 5：提交第一阶段可运行验收。**

```powershell
git add examples README.md pom.xml
git commit -m "feat: add tenant isolation demo and end to end tests"
```

## 计划自审

### 规格覆盖

- Java 21、Maven、BOM、版本治理和 CI：Task 1。
- 纯 Java `base` 契约、错误模型和分页：Task 2。
- 租户上下文、缺失租户拒绝和异步前的清理基础：Task 3。
- 账号密码体系所需的认证主体、JWT 资源服务器和权限版本：Task 4；登录与刷新令牌的 IAM 服务属于后续平台服务计划。
- 全局异常与 `traceId`：Task 5。
- 共享表 `tenant_id`、部门及下级数据范围、跨租户读写隔离：Task 6。
- 仅 Redis 的租户安全缓存键：Task 7。
- 可运行的单服务验收和 Flyway 基线：Task 8。
- RabbitMQ、Outbox、审计、Nacos、网关、可观测性及平台服务：按设计文档明确延后，分别制定独立计划。

### 一致性检查

本计划中的 `TenantId`、`TenantContext`、`AuthenticatedPrincipal`、`DataScope`、`CacheKey` 与错误码名称在所有任务中保持一致。业务服务不直接使用中间件客户端 API；所有租户查询都以 `TenantContext.require()` 为根。每个实现任务均先写失败测试、确认失败、最小实现、确认通过并提交。
