# web-core 类说明

## `com.sherlock.framework.web.error`

### `FrameworkErrorHandler`

最低优先级 `@RestControllerAdvice`。将业务异常、Spring MVC 错误、`@ResponseStatus` 异常和未知异常转换为安全的 `ApiResponse`；复制 `ErrorResponse` 响应头，并只在服务端记录未知异常堆栈。

### `FrameworkErrorHandler.FieldValidationError`

字段校验错误响应项，只暴露字段名和通用消息，不回显被拒绝的原始值或校验器内部消息。

## `com.sherlock.framework.web.trace`

### `TraceIdResolver`

解析并校验 `X-Trace-Id`。仅接受长度不超过 64 的字母、数字和连字符；否则生成 UUID。优先读取请求属性，保证同一请求复用同一 trace ID。

### `TraceIdFilter`

最高优先级请求过滤器。写入请求属性、响应 `X-Trace-Id` 和 MDC，并在 `finally` 清理 MDC；不涉及租户或认证主体。
