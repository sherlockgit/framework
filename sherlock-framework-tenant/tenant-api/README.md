# tenant-api

定义租户 ID 与 `TenantContext`。数据、缓存和认证模块使用该契约获取当前租户，缺失租户的受管操作必须失败。

`TenantContext` 基于 `ThreadLocal`；请求结束必须清理，异步任务必须使用 `TenantContextSnapshot` 或 `executeWith` 显式传播并恢复上下文。`require()` 缺少上下文时抛出 `TENANT-400-001`。

生产类设计见 [docs/classes.md](docs/classes.md)。
