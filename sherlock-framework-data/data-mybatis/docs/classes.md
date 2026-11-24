# data-mybatis 类说明

## `com.sherlock.framework.data.mybatis`

### `TenantTablePolicy`

维护全局表和显式租户表策略。除全局表外默认均为租户表；启动时校验所有已映射的非全局表具有 `tenant_id` 映射。

### `FrameworkTenantLineHandler`

MyBatis-Plus `TenantLineHandler` 实现。固定租户列为 `tenant_id`，值来自 `TenantContext.require()`；全局表才允许跳过租户条件。

### `FrameworkTenantLineInnerInterceptor`

扩展 MyBatis-Plus 租户拦截器，保护租户归属不可变。拒绝受管表显式 `tenant_id` 写入或更新，并拒绝无法安全注入租户列的无目标列 INSERT 形式。

### `FrameworkDataPermissionHandler`

生成数据范围 SQL AST。非 `ALL_TENANT` 时追加本人 `creator_id` 或可见 `department_id` 条件；空范围生成 `1 = 0` 拒绝条件。
