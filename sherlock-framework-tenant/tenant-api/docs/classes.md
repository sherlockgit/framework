# tenant-api 类说明

## `com.sherlock.framework.tenant`

### `TenantId`

租户 ID 值对象，只接受正整数。它是租户隔离键、SQL 条件和认证主体中的租户标识。

### `TenantContext`

当前线程的租户上下文。提供读取、强制读取、设置、清理和临时绑定执行能力；`executeWith` 结束后恢复之前上下文，避免嵌套或线程复用污染。

### `TenantContextSnapshot`

租户上下文快照，用于显式捕获和恢复当前租户。快照为空时恢复操作会清理上下文。
