# data-spring-boot-starter 类说明

## `com.sherlock.framework.data.autoconfigure`

### `DataProperties`

绑定 `sherlock.data` 配置，包含 `globalTables` 和 `tenantTables`。集合会复制，避免外部可变引用。

### `DataAutoConfiguration`

注册表策略、MyBatis-Plus 插件、映射校验器和 Servlet 清理过滤器。启动阶段验证插件唯一性、策略身份和顺序，将租户与数据权限插件置于分页插件之前。

## `com.sherlock.framework.data.web`

### `DataScopeContextCleanupFilter`

最高优先级 Servlet 过滤器。请求入口和退出时清理 `DataScopeContext`，防止线程复用继承前一请求的数据范围。
