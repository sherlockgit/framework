# sherlock-framework-base 类说明

## `com.sherlock.framework.base.error`

### `ErrorCode`

稳定业务错误码模型，包含 `value` 和面向调用方的默认消息。两个字段都不能为空或空白，避免异常响应出现无语义错误信息。

### `BusinessException`

承载 `ErrorCode` 的运行时业务异常。异常消息固定取自 `ErrorCode.defaultMessage()`，Web 层据此生成稳定错误响应。

## `com.sherlock.framework.base.page`

### `PageRequest`

分页输入模型。`pageNumber` 从 1 开始，`pageSize` 限制在 1 至 500，避免无界分页请求。

### `PageResult<T>`

分页输出模型，包含记录、总数和当前分页参数。构造时复制并只读化记录列表，防止调用方修改结果内容；总数不得为负。

## `com.sherlock.framework.base.web`

### `ApiResponse<T>`

统一 API 响应模型，字段为成功标识、错误码、消息、数据和 `traceId`。`success` 与 `failure` 工厂方法分别构造成功和失败响应；不涉及 HTTP 状态或线程上下文。
