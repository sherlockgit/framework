# tenant-demo 类说明

## `com.sherlock.framework.example`

### `TenantDemoApplication`

示例应用入口。扫描订单 Mapper，配置资源服务安全链，将 `PrincipalContextFilter` 放在 Bearer 认证之后，并将本人数据范围过滤器放在主体绑定之后。示例中禁止重复注册该过滤器为普通 Servlet Filter。

### `TenantDemoApplication.SelfDataScopeFilter`

示例内部过滤器，为每个已认证请求绑定 `DataScope.self()`，并在 `finally` 清理。它展示业务服务如何在认证后选择自己的数据范围策略。

## `com.sherlock.framework.example.order`

### `OrderRow`

订单表实体映射，包含 `tenant_id`、业务键、创建人、部门、名称和创建时间。`tenant_id` 是框架启动校验所要求的租户列映射。

### `OrderMapper`

订单 MyBatis Mapper。插入语句显式列出业务列但不含 `tenant_id`，让框架租户拦截器安全注入；查询只按业务键表达，租户与数据范围由拦截器追加。

### `OrderController`

订单 HTTP 控制器。创建和读取前都先经 Mapper 执行租户与数据范围授权，再读写 `CachePort`，因此缓存不会绕过授权判断。响应统一使用 `ApiResponse`。

### `OrderController.CreateOrderRequest`

创建订单请求模型，包含业务键、部门和名称；控制器拒绝空业务键、空名称和非正部门 ID。

### `OrderController.OrderView`

订单对外视图，只暴露业务键和名称，不暴露租户、创建人或部门等内部隔离字段。
