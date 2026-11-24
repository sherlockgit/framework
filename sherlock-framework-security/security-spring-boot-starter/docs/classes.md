# security-spring-boot-starter 类说明

## `com.sherlock.framework.security.autoconfigure`

### `SecurityAutoConfiguration`

Servlet 条件自动装配类。创建可选 `JwtDecoder`、`StrictJwtValidator`、JWT 配置校验器、认证转换器和主体上下文过滤器。认证转换器在权限映射前执行严格校验。

### `SecurityJwtProperties`

绑定 `sherlock.security.jwt` 配置，包含签发者、受众和 JWK Set 地址；JWT 启用时要求签发者与受众完整。

## `com.sherlock.framework.security.web`

### `StrictJwtValidator`

校验签发者、受众、过期、生效时间、`sub`、`tenant_id`、`policy_version` 和 `jti`；数值声明必须是精确整数，租户与用户为正数，策略版本可为 0。

### `PrincipalContextFilter`

资源服务认证后的 Servlet 过滤器。将已验证 JWT 或框架主体写入 `PrincipalContext` 和 `TenantContext`，并在 `finally` 清理两者；保留终检以防配置错误。
