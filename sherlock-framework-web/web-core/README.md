# web-core

提供请求级 `traceId`、MDC 关联及安全的统一异常响应。错误响应使用 `ApiResponse`，不向客户端泄露堆栈、异常详情或 `ResponseStatusException.reason`。

- `BusinessException` 返回 400。
- MVC 中的认证与授权异常映射为 401、403；安全过滤链自身的 401、403 不被本模块抢占。
- Spring `ErrorResponse` 保留状态码和必要响应头，例如 405 的 `Allow`。
- 未处理异常记录带 `traceId` 的服务端日志，客户端仅收到 `SYS-500-001`。

生产类设计见 [docs/classes.md](docs/classes.md)。
