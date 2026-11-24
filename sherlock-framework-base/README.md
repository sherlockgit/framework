# sherlock-framework-base

## 职责

提供不依赖 Spring 的基础模型：稳定错误码、业务异常、统一 API 响应和分页请求/结果。它是租户、安全、Web、数据和缓存模块的底层依赖。

## 边界

不包含 HTTP 处理、序列化策略或 Spring 自动装配；`ApiResponse` 只定义响应数据结构，HTTP 状态映射由 `web-core` 负责。

## 测试策略

覆盖错误码非空、响应工厂方法、分页参数范围及结果列表防御性复制。

生产类设计见 [docs/classes.md](docs/classes.md)。
