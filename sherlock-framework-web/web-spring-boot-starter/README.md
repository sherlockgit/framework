# web-spring-boot-starter

在 Servlet Web 应用中自动注册 `TraceIdResolver`、`TraceIdFilter` 和 `FrameworkErrorHandler`。用户提供同类型 Bean 时，默认实现不会覆盖用户配置；非 Servlet 应用不加载 Web Bean。

生产类设计见 [docs/classes.md](docs/classes.md)。
