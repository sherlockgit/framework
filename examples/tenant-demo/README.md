# tenant-demo

## 目标

最小多租户订单服务，演示 JWT 认证、租户上下文、数据范围、MyBatis 共享表隔离、Flyway 和 Redis 租户缓存如何组合。

## 接口

- `POST /orders`：创建订单，需要 `order:write`。
- `GET /orders/{businessKey}`：按业务键读取订单，需要 `order:read`。

订单表唯一键为 `(tenant_id, business_key)`。Mapper 插入语句不携带 `tenant_id`，由框架租户拦截器自动注入当前租户。

## 验证

Docker 环境中的 `TenantIsolationE2ETest` 启动 MySQL 和 Redis，验证两个租户可使用相同业务键、HTTP/MyBatis/Redis 三层均隔离，且 JWT 权限、租户声明和本人数据范围受约束。

H2 测试仅用于 Spring 上下文、Flyway 和 Mapper 自动注入的补充回归，不替代 MySQL、Redis、HTTP 或 JWT 端到端验收。

生产类设计见 [docs/classes.md](docs/classes.md)。
