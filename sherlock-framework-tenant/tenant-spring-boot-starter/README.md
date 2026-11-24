# tenant-spring-boot-starter

在 Servlet Web 应用中注册租户上下文清理过滤器。它不解析请求头或 JWT，只防御线程复用造成的残留租户。用户可提供同类型 Bean 覆盖默认实现。

生产类设计见 [docs/classes.md](docs/classes.md)。
