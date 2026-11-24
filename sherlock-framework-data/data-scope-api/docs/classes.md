# data-scope-api 类说明

## `com.sherlock.framework.data.scope`

### `DataScopeType`

数据范围枚举：`SELF`、`DEPARTMENTS`、`MANAGED_DEPARTMENTS` 和 `ALL_TENANT`。

### `DataScope`

不可变数据范围模型，保存范围类型、可见部门和是否包含本人。`merge` 合并多个角色范围，`ALL_TENANT` 优先，其他部门 ID 取并集；部门 ID 必须为正数。

### `DataScopeContext`

当前线程的数据范围。提供读取、强制读取、清理、快照和临时绑定执行；异步任务结束后恢复之前的数据范围。

### `DataScopeResolver`

根据 `PrincipalContext` 和外部策略快照函数解析当前主体的数据范围。`resolveAndBind` 将结果写入 `DataScopeContext`，调用方必须确保请求结束清理。
