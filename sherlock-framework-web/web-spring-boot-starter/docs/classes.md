# web-spring-boot-starter 类说明

## `com.sherlock.framework.web.autoconfigure`

### `WebAutoConfiguration`

Servlet 条件自动装配类。缺少用户同类型 Bean 时，注册 `TraceIdResolver`、`TraceIdFilter` 和 `FrameworkErrorHandler`。
