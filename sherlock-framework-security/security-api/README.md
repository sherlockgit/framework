# security-api

提供认证主体、权限值对象与 `PrincipalContext`，不依赖 Spring Security。认证主体不可变，包含用户、租户、权限集合和策略版本；`PrincipalContext` 基于 `ThreadLocal`，必须在请求或异步任务结束时清理。

生产类设计见 [docs/classes.md](docs/classes.md)。
