# security-spring-boot-starter

## 职责

为 Servlet 服务提供 JWT 资源服务集成、权限映射、严格声明校验，以及 `PrincipalContext` 和 `TenantContext` 绑定。

## 配置

使用 `sherlock.security.jwt.issuer`、`sherlock.security.jwt.audience` 和可选 `sherlock.security.jwt.jwk-set-uri`。只要任一 JWT 配置出现，签发者和受众必须同时配置，否则应用启动失败。`permissions` 声明映射为无前缀的 Spring Security authority，例如 `order:read`。

## 安全边界

严格校验在 `JwtAuthenticationConverter` 中、授权之前执行，因此无论使用 Nimbus、Spring Boot 还是用户提供的 `JwtDecoder`，错误受众、时间或主体声明都会返回 401；合法但权限不足才返回 403。

生产类设计见 [docs/classes.md](docs/classes.md)。
