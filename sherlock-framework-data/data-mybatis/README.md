# data-mybatis

## 职责

基于 MyBatis-Plus 为共享表多租户提供租户条件、数据范围条件和租户归属写入保护。

## 安全约束

- 非全局表默认视为租户表，映射必须包含 `tenant_id`。
- 查询、更新和删除始终受 `tenant_id` 约束；`ALL_TENANT` 只取消数据范围谓词，不取消租户谓词。
- 受管表禁止显式写入或更新 `tenant_id`；无目标列的 `INSERT`、`INSERT ... SELECT` 和 `INSERT ... SET` 直接拒绝，避免 MyBatis-Plus 跳过注入。
- 数据范围为空时生成拒绝条件，不会扩大查询范围。

真实 MySQL 行为由 Testcontainers 验证，需要 Docker。

生产类设计见 [docs/classes.md](docs/classes.md)。
