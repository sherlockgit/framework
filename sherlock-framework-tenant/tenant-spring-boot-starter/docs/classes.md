# tenant-spring-boot-starter 类说明

## `com.sherlock.framework.tenant.autoconfigure`

### `TenantAutoConfiguration`

Servlet 条件自动装配类。缺少用户自定义 `TenantContextCleanupFilter` 时注册默认过滤器。

## `com.sherlock.framework.tenant.web`

### `TenantContextCleanupFilter`

最高优先级 Servlet 过滤器。进入链路前和退出链路后均清理 `TenantContext`，内部过滤器短路或抛异常时也不会保留租户信息。
