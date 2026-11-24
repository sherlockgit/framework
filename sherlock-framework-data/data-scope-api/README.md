# data-scope-api

定义本人、部门、管理部门与全租户四类数据范围，以及范围合并和线程上下文契约。

`DataScopeContext` 基于 `ThreadLocal`；请求结束必须清理，异步或消息任务需使用 `snapshot` 与 `executeWith` 显式传播和恢复。角色范围合并时 `ALL_TENANT` 优先，其他部门集合取并集。

生产类设计见 [docs/classes.md](docs/classes.md)。
