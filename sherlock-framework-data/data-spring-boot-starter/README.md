# data-spring-boot-starter

## 职责

自动配置 `TenantTablePolicy`、MyBatis-Plus 隔离插件、表映射启动校验和 `DataScopeContext` 清理过滤器。

## 配置

使用 `sherlock.data.global-tables` 声明全局表，使用 `sherlock.data.tenant-tables` 增加必须显式校验的租户表。平台默认全局表为 `flyway_schema_history`。

## 插件约束

框架确保唯一的租户拦截器和数据权限拦截器位于分页插件之前。若已有第三方租户或数据权限插件、重复插件，或框架插件使用不同的 `TenantTablePolicy`，应用启动失败，避免静默叠加产生绕过。

生产类设计见 [docs/classes.md](docs/classes.md)。
