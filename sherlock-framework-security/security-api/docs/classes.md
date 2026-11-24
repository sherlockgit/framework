# security-api 类说明

## `com.sherlock.framework.security`

### `AuthenticatedPrincipal`

已认证主体模型，包含用户、租户、权限集合和权限策略版本。权限集合会复制为不可变集合，策略版本允许从 0 开始。

### `Permission`

权限字符串值对象，用于校验单个权限标识不为空。

### `PrincipalContext`

当前线程认证主体上下文。缺少主体时 `require()` 抛出 `SECURITY-401-001`；必须在请求或异步任务结束时清理。
